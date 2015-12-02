var Hijax = (function ($, Hijax) {

  var my = {

    datasets : {},
    bloodhounds : {},

    fillDatasetRecursive : function(dataset, scope, depth) {
      var property_name = scope.hasOwnProperty('hasTopConcept') ? 'hasTopConcept' : 'narrower';

      for(var i = 0; i < scope[ property_name ].length; i++) {
        dataset.push({
          id : scope[ property_name ][ i ]['@id'],
          name : scope[ property_name ][ i ].name[0]['@value'],
          depth : depth
        });
        if( scope[ property_name ][ i ].hasOwnProperty('narrower') ) {
          my.fillDatasetRecursive(dataset, scope[ property_name ][ i ], depth + 1);
        }
      }

    },

    getDataset : function(lookup_url) { console.log('getDataset', lookup_url);
      if( my.datasets[lookup_url] ) {
        return my.datasets[lookup_url];
      } else {

  	    $.ajax({
    	    url: lookup_url,
    	    headers: {
      	    Accept: 'application/json'
    	    },
    	    success: function(data){
            var dataset = [];
      	    my.fillDatasetRecursive(dataset, data, 0);
      	    my.datasets[ lookup_url ] = dataset;
      	    return my.datasets[ lookup_url ];
      	  },
      	  error: function(jqXHR, status, error){
        	  console.log('error', jqXHR, status);
      	  }
        });

      }
    },

    getBloodhound : function(lookup_url) { console.log('getBloodhound', lookup_url);
      if( my.bloodhounds[lookup_url] ) {
        return my.bloodhounds[lookup_url];
      } else {

          my.bloodhounds[ lookup_url ] = new Bloodhound({
            datumTokenizer: function(d){
              return Bloodhound.tokenizers.whitespace(d.name);
            },
            queryTokenizer: Bloodhound.tokenizers.whitespace,
            local: my.getDataset( lookup_url ),
            identify: function(result){
              return result.id;
            },
          });

          return my.bloodhounds[ lookup_url ];
      }
    },

    attach : function(context) {

      // my.getBloodhound('/assets/json/esc.json');

      my.templates = {
        'item' : Handlebars.compile($('#Resource_item\\.mustache').html()),
        'select' : Handlebars.compile($('#Resource_select\\.mustache').html())
      }

      // iterate over widgets

      $('[data-behaviour="resource"]', context).each(function() {

        var widget = $(this);
        var lookup_url = $(this).data('lookup'); console.log(lookup_url);

        $(this).addClass('behaving');

        // create template

        var multiple_one_template = Handlebars.compile(
          widget.find('.multiple-one').last().clone()
            .remove('script')[0].outerHTML
            .replace(
              /\[\d+\]/g,
              '[{{index}}]'
            )
        );

        // if more than one remove the last one

        if( widget.find('.multiple-one').length > 1 ) {
          widget.find('.multiple-one').last().remove();
        }

        // init each

        widget.find('.multiple-one').each(function(){
          my.initOne(this);
        });

        // append add control

        $(widget).append(
          my.templates.select({
            subject : widget.find('h4').text()
          })
        );

        var dropdown = $(widget).find('.dropdown');
        var dropdown_button = dropdown.find('button');
        var dropdown_menu = dropdown.find('.dropdown-menu')
        var typeahead = dropdown.find('.typeahead');

        // prevent dropdown from being closed on click

        dropdown_menu.on('click', function(){
          return false; // dirty
        });

        // init typeahead

        var dataset = my.getDataset( lookup_url );
        var bloodhound = my.getBloodhound( lookup_url );

        typeahead.typeahead({
          hint: false,
          highlight: true,
          minLength: 0
        },{
          name: 'xyz',
          limit: 9999,
          display: 'name',
          source: function(q, sync){
            if (q === '') {
              sync(dataset);
            } else {
              bloodhound.search(q, sync);
            }
          }
        });

        // hack to initially open the typeahead suggestion list

        dropdown.on('shown.bs.dropdown', function(){
          typeahead.typeahead('val', 'x');
          typeahead.typeahead('val', '');
        });

        // when selection is made ...

        typeahead.bind('typeahead:select', function(e, suggestion) {
          console.log('selection made');
/*
          language_button.dropdown('toggle');
          typeahead.typeahead('val', '');
          language_button.find('.text').text(suggestion.label);
          language_input.val(suggestion.id);
*/
        });

      });

    },

    initOne : function(one) {

      // reorganize layout

      var raw_json = $(one).find('script').html();

      if(raw_json) {
        var json_data = JSON.parse( raw_json );

        $(one).append(
          my.templates['item'](json_data)
        );

        $(one).find('[data-action="delete"]').click(function(){
          $(one).fadeOut(function(){
            $(this).remove();
          });
        })
      }

    }
  };

  Hijax.behaviours.Resource = my;
  return Hijax;

})(jQuery, Hijax);
