var Hijax = (function ($, Hijax) {

  var my = {

    templates : {},

    attach : function(context) {

      my.templates['multiple-one'] = Handlebars.compile($('#select_multiple-one\\.mustache').html());

      // prepare countries array

      my.countries_array = [];

      for(i in i18nStrings.countries) {
        my.countries_array.push({
          id: i,
          label: i18nStrings.countries[i]
        });
      }

      my.countries_array.sort(function(a,b) {
        if(a.label < b.label) return -1;
        if(a.label == b.label) return 0;
        if(a.label > b.label) return 1;
      });

      // init countries bloodhound

      my.countries_bloodhoud = new Bloodhound({
        datumTokenizer: function(d){
          return Bloodhound.tokenizers.whitespace(d.label);
        },
        queryTokenizer: Bloodhound.tokenizers.whitespace,
        local: my.countries_array,
        identify: function(result){
          return result.id;
        },
      });

      // iterate over widgets

      $('[data-behaviour="select"]', context).each(function() {

        var widget = $(this);

        $(this).addClass('behaving');

        if( widget.find('.multiple-list').length ) {

          // create fieldset template

          var multiple_one_template = Handlebars.compile(
            widget.find('.multiple-one').last()[0].outerHTML.replace(
              /\[\d+\]/g,
              '[{{index}}]'
            )
          );

          // if more than one, remove the last one

          if( widget.find('.multiple-one').length > 1 ) {
            widget.find('.multiple-one').last().remove();
          }

          // append add control

          $('<span class="small" data-action="add">+ Add ' + widget.find('.multiple-list').attr('title') + '</span>')
            .appendTo(widget)
            .click(function(){
              var multiple_one_new = $( multiple_one_template({ index : widget.find('.multiple-one').length }) );
              widget.find('.multiple-list').append( multiple_one_new );
              my.initOne( multiple_one_new );
            });

        }

        // init each

        widget.find('.multiple-one, .single').each(function(){
          my.initOne(this);
        });

      });

    },

    initOne : function(one) {

      var input = $(one).find('input');

      $(one).append( my.templates['multiple-one']() );

      // init country typeahead

      var typeahead = $(one).find('.typeahead');
      var dropdown = $(one).find('.dropdown');
      var dropdown_button = dropdown.find('button');
      var dropdown_menu = dropdown.find('.dropdown-menu');

      var current_country_code = input.val();

      if(current_country_code != "") {
        dropdown_button.find('.text').text(
          i18nStrings.countries[ current_country_code ]
        );
      }

      dropdown_menu.on('click', function(){
        return false; // dirty
      });

      typeahead.typeahead({
        hint: false,
        highlight: true,
        minLength: 0
      },{
        name: 'countries',
        limit: 9999,
        display: 'label',
        source: function(q, sync){
          if (q === '') {
            sync(my.countries_array);
          } else {
            my.countries_bloodhoud.search(q, sync);
          }
        }
      });

      // hack to initially open the typeahead suggestion list

      dropdown.on('shown.bs.dropdown', function(){
        typeahead.focus();
        typeahead.typeahead('val', 'x');
        typeahead.typeahead('val', '');
      });

      // when selection is made ...

      typeahead.bind('typeahead:select', function(e, suggestion) {
        dropdown_button.dropdown('toggle');
        typeahead.typeahead('val', '');
        input.val(
          suggestion.id
        );
        dropdown_button.find('.text').text(
          suggestion.label
        );
      });

    }

  };

  Hijax.behaviours.select = my;
  return Hijax;

})(jQuery, Hijax);
