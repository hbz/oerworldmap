var Hijax = (function ($, Hijax) {

  var templates;
  var languages_array;
  var languages_bloodhoud;

  var my = {

    attach : function(context) {

      templates = {
        'input-group' : Handlebars.compile($('#LocalizedInput_input-group\\.mustache').html())
      }

      // prepare languages array

      my.languages_array = [];

      for(i in i18nStrings.languages) {
        my.languages_array.push({
          id: i,
          label: i18nStrings.languages[i]
        });
      }

      my.languages_array.sort(function(a,b) {
        if(a.label < b.label) return -1;
        if(a.label == b.label) return 0;
        if(a.label > b.label) return 1;
      });

      // init languages bloodhound

      my.languages_bloodhoud = new Bloodhound({
        datumTokenizer: function(d){
          return Bloodhound.tokenizers.whitespace(d.label);
        },
        queryTokenizer: Bloodhound.tokenizers.whitespace,
        local: my.languages_array,
        identify: function(result){
          return result.id;
        },
      });

      // iterate over widgets

      $('[data-behaviour="localizedInput"]', context).each(function() {

        var widget = $(this);

        $(this).addClass('behaving');

        // create langauge fieldset template

        var language_fieldset_template = Handlebars.compile(
          widget.find('.i18n').last()[0].outerHTML.replace(
            /\[\d+\]/g,
            '[{{index}}]'
          )
        );

        // if more than one language fieldset, remove the last one

        if( widget.find('.i18n').length > 1 ) {
          widget.find('.i18n').last().remove();
        }

        // init language fieldsets

        widget.find('.i18n').each(function(){
          my.initLanguageFieldset(this);
        });

        // append "add language" control

        $('<span class="add-link small">+ Add Language</span>')
          .appendTo(widget)
          .click(function(){
            var new_i18n = $( language_fieldset_template({ index : widget.find('.i18n').length }) );
            widget.find('.i18n-list').append( new_i18n );
            my.initLanguageFieldset( new_i18n );
          });

      });

    },

    initLanguageFieldset : function(i18n) {

      var fieldset = $(i18n).find('fieldset'); console.log(fieldset);

      // reorganize layout

      var value_input_html = fieldset
        .find('[name*="@value"]')
        .addClass('form-control')
        .detach()[0].outerHTML;

      var current_language_code = fieldset.find('[name*="@language"]').val();
      var button_text = current_language_code ? i18nStrings.languages[ current_language_code ] : 'Language';

      fieldset.append(
        templates['input-group']({
          'value_input_html' : value_input_html,
          'button_text' : button_text
        })
      );

      // save controls

      var language_button = fieldset.find('button');
      var dropdown = fieldset.find('.dropdown-menu')
      var dropdown_parent = dropdown.parent();
      var language_input = fieldset.find('[name*="@language"]');
      var typeahead = fieldset.find('.typeahead');

      // prevent dropdown from being closed on click

      dropdown.on('click', function(){
        return false; // dirty
      });

      // init typeahead

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
            sync(my.languages_array);
          } else {
            my.languages_bloodhoud.search(q, sync);
          }
        }
      });

      // hack to initially open the typeahead suggestion list

      dropdown_parent.on('shown.bs.dropdown', function(){
        typeahead.typeahead('val', 'x');
        typeahead.typeahead('val', '');
      });

      // when selection is made ...

      typeahead.bind('typeahead:select', function(e, suggestion) {
        language_button.dropdown('toggle');
        typeahead.typeahead('val', '');
        language_button.find('.text').text(suggestion.label);
        language_input.val(suggestion.id);
      });
    }
  };

  Hijax.behaviours.localizedInput = my;
  return Hijax;

})(jQuery, Hijax);
