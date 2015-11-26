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

          // prepare languages array

          var languages_array = [];

          for(i in i18nStrings.languages) {
            languages_array.push({
              id: i,
              label: i18nStrings.languages[i]
            });
          }

          languages_array.sort(function(a,b) {
            if(a.label < b.label) return -1;
            if(a.label == b.label) return 0;
            if(a.label > b.label) return 1;
          });

          // reorganize layout

          var value_input_html = $(fieldset)
            .find('[name*="@value"]')
            .addClass('form-control')
            .detach()[0].outerHTML;

          var current_language_code = $(fieldset).find('[name*="@language"]').val();

          var button_text = current_language_code ? i18nStrings.languages[ current_language_code ] : 'Language';
          console.log(current_language_code, button_text);
          $(fieldset).append(
            templates['input-group']({
              'value_input_html' : value_input_html,
              'button_text' : button_text
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



          var languages = new Bloodhound({
            datumTokenizer: function(d){
              return Bloodhound.tokenizers.whitespace(d.label);
            },
            queryTokenizer: Bloodhound.tokenizers.whitespace,
            local: languages_array,
            identify: function(result){
              return result.id;
            },
          });

          var typeahead = $(fieldset).find('.typeahead');

          typeahead.typeahead({
            hint: false,
            highlight: true,
            minLength: 0
          },{
            name: 'languages',
            limit: 9999,
            display: 'label',
            source: function(q, sync){
              if (q === '') {
                sync(languages_array);
              } else {
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
            language_button.find('.text').text(suggestion.label); console.log(suggestion.id,language_input);
            language_input.val(suggestion.id);
          });

        });
      });

    }
  };

  Hijax.behaviours.localizedInput = my;
  return Hijax;

})(jQuery, Hijax);
