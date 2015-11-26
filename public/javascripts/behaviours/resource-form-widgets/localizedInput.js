var Hijax = (function ($, Hijax) {

  var my = {

    attach : function(context) {

      templates = {
        'input-group' : Handlebars.compile($('#LocalizedInput_input-group\\.mustache').html())
      }

      $('[data-behaviour="localizedInput"]', context).each(function() {
        $(this).addClass('behaving');

        $(this).find('fieldset').each(function(){

          var fieldset = this;

          // enhance layout

          var value_input = $(fieldset)
            .find('[name*="@value"]')
            .addClass('form-control')
            .detach()[0].outerHTML;

          $(fieldset).append(
            templates['input-group']({
              'value_input' : value_input,
              'languages' : i18nStrings.countries
            })
          );

          // save controls

          var language_button = $(fieldset).find('button');
          var dropdown = $(fieldset).find('.dropdown-menu')
          var dropdown_parent = dropdown.parent();
          var language_input = $(fieldset).find('[name*="@language"]');

          // init typeahead

          $(fieldset).find('.dropdown-menu').on('click', function(){
            return false; // dirty
          });

          var languages_array = [];

          for(i in i18nStrings.countries) {
            languages_array.push( i18nStrings.countries[i] );
          }

          var languages = new Bloodhound({
            datumTokenizer: Bloodhound.tokenizers.whitespace,
            queryTokenizer: Bloodhound.tokenizers.whitespace,
            local: languages_array
          });

          var typeahead = $(fieldset).find('.typeahead');

          typeahead.typeahead({
            hint: false,
            highlight: true,
            minLength: 0
          },{
            name: 'languages',
            limit: 9999,
            source: function(q, sync){
              if (q === '') {
                sync(languages_array);
              }
              else {
                languages.search(q, sync);
              }
            }
          });

          dropdown_parent.on('shown.bs.dropdown', function(){
            // hack to initially open the typeahead suggestion list
            typeahead.typeahead('val', 'x');
            typeahead.typeahead('val', '');
          });

          typeahead.bind('typeahead:select', function(e, suggestion) {
            language_button.dropdown('toggle');
            typeahead.typeahead('val', '');
            language_button.find('.text').text(suggestion);
            language_input.val(suggestion);
          });

        });
      });

    }
  };

  //Hijax.behaviours.localizedInput = my;
  return Hijax;

})(jQuery, Hijax);
