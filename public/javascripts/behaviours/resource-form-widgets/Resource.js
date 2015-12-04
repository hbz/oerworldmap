var Hijax = (function ($, Hijax) {

  var my = {

    templates : {},
    datasets : {},
    bloodhounds : {},
    bloodhoundDefers : {},

    fillDatasetRecursive : function(dataset, scope, depth) {
      var property_name = scope.hasOwnProperty('hasTopConcept') ? 'hasTopConcept' : 'narrower';

      for(var i = 0; i < scope[ property_name ].length; i++) {
        scope[ property_name ][ i ][ 'depth' ] = depth;
/*
        dataset.push({
          id : scope[ property_name ][ i ]['@id'],
          name : scope[ property_name ][ i ].name[0]['@value'],
          depth : depth
        });
*/
        dataset.push( scope[ property_name ][ i ] );
        if( scope[ property_name ][ i ].hasOwnProperty('narrower') ) {
          my.fillDatasetRecursive(dataset, scope[ property_name ][ i ], depth + 1);
        }
      }

    },

    createDataset : function(lookup_url) {
      var dfd = $.Deferred();

      if( my.datasets[ lookup_url ] ) {

        dfd.resolve();

      } else { console.log('ajax getting dataset', lookup_url);

  	    return $.ajax({
    	    url: lookup_url,
    	    headers: {
      	    Accept: 'application/json'
    	    },
    	    success: function(data){
            var dataset = [];
      	    my.fillDatasetRecursive(dataset, data, 0);
      	    my.datasets[ lookup_url ] = dataset;
      	  },
      	  error: function(jqXHR, status, error){
        	  console.log('error', jqXHR, status);
      	  }
        });

      }
    },

    createBloodhound : function(lookup_url) { console.log('getBloodhound', lookup_url);
      var dfd = $.Deferred();

      if( my.bloodhounds[lookup_url] ) {

        dfd.resolve();

      } else {

          $.when(
            my.createDataset( lookup_url )
          ).then(function(){

            my.bloodhounds[ lookup_url ] = new Bloodhound({
              datumTokenizer: function(d){
                return Bloodhound.tokenizers.whitespace( d.name[0]['@value'] );
              },
              queryTokenizer: Bloodhound.tokenizers.whitespace,
              local: my.datasets[ lookup_url ],
              identify: function(result){
                return result['@id'];
              },
            });

            dfd.resolve();

          });
      }

      return dfd.promise();
    },

    gotBloodhound : function(lookup_url) {
      if( ! my.bloodhoundDefers[ lookup_url ] ) {
        my.bloodhoundDefers[ lookup_url ] = my.createBloodhound( lookup_url );
      }
      return my.bloodhoundDefers[ lookup_url ];
    },

    attach : function(context) {

      // my.getBloodhound('/assets/json/esc.json');

      my.templates['item'] = Handlebars.compile($('#Resource_item\\.mustache').html());
      my.templates['select'] = Handlebars.compile($('#Resource_select\\.mustache').html());
      my.templates['multiple_one'] = Handlebars.compile($('#Resource_multiple-one\\.mustache').html());

      // iterate over widgets

      $('[data-behaviour="resource"]', context).each(function() {

        var widget = $(this);
        var lookup_url = $(this).data('lookup');
        var key = $(this).data('key');

        if( ! lookup_url.endsWith('json') ) { return; }

        $(this).addClass('behaving');

        // create template

/*
        my.templates[ 'multiple_one' ] = Handlebars.compile(
          widget.find('.multiple-one').last().clone()
            .remove('script')[0].outerHTML
            .replace(
              /\[\d+\]/g,
              '[{{index}}]'
            ).replace(
              /<script\b[^>]*>([\s\S]*?)<\/script>/gm,
              '[{{json}}]'
            )
        ); console.log( my.templates );
*/

        // if more than one remove the last one

        if( widget.find('.multiple-one').length > 1 ) {
          widget.find('.multiple-one').last().remove();
        }

        // init each

        widget.find('.multiple-one').each(function(){
          my.initOne(this);
        });

        // append add control

        widget.append(
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

        $.when(
          my.gotBloodhound( lookup_url )
        ).then(function(){

          typeahead.typeahead({
            hint: false,
            highlight: true,
            minLength: 0
          },{
            name: 'xyz',
            limit: 9999,
            // display: 'name',
            source: function(q, sync){
              if (q === '') {
                sync(my.datasets[ lookup_url ]);
              } else {
                my.bloodhounds[ lookup_url ].search(q, sync);
              }
            },
            templates: {
              suggestion: Handlebars.compile('<div><div class="depth-{{depth}}"><i class="fa fa-fw fa-tag"></i> {{name.[0].[@value]}}</div></div>')
            }
          });

        });

        // hack to initially open the typeahead suggestion list

        dropdown.on('shown.bs.dropdown', function(){
          typeahead.typeahead('val', 'x');
          typeahead.typeahead('val', '');
        });

        // when selection is made ...
        console.log(my.templates);
        typeahead.bind('typeahead:select', function(e, suggestion) {
          dropdown_button.dropdown('toggle');
          typeahead.typeahead('val', '');

          var highest_index = + widget
            .find('.multiple-one').last().find('input').attr('name')
            .match(/\[\d+\]/);

          console.log( suggestion );
          // return;

          suggestion['key'] = key;
          suggestion['@index'] = highest_index + 1;

          var one_new = my.templates[ 'multiple_one' ]( suggestion );

          widget.find('.multiple-list').append( one_new );

          my.initOne( one_new );
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
