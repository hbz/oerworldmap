var Hijax = (function ($, Hijax) {

  var templates;
  var languages_array;
  var languages_bloodhoud;

  var my = {

    attach : function(context) {

      my.templates = {
        'input-group' : Handlebars.compile($('#localizedInput_input-group\\.mustache').html())
      }

      // prepare languages array

      my.languages_array = [];

      for(i in i18nStrings["iso639-1"]) {
        my.languages_array.push({
          id: i,
          label: i18nStrings["iso639-1"][i]
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
          return Bloodhound.tokenizers.whitespace(
            bloodhoundAccentFolding.normalize(d.label)
          );
        },
        queryTokenizer: bloodhoundAccentFolding.queryTokenizer,
        local: my.languages_array,
        identify: function(result){
          return result.id;
        },
      });

      // iterate over widgets

      $('[data-behaviour~="localizedInput"]', context)
        .not('[data-dont-behave] [data-behaviour~="localizedInput"]')
        .each(function()
      {

        var widget = $(this);

        $(this).addClass('behaving');

        // create i18n template

        var multiple_one_template = Handlebars.compile(
          widget.find('.multiple-one').last()[0].outerHTML.replace(
            /\[\d+\]/g,
            '[{{index}}]'
          )
        );

        // if more than one i18n, remove the last one

        if( widget.find('.multiple-one').length > 1 ) {
          widget.find('.multiple-one').last().remove();
        }

        // init i18n

        widget.find('.multiple-one').each(function(){
          my.initOne(this);
        });

        // append "add language" control

        $('<span class="small" data-action="add">+ Add Language</span>')
          .appendTo(widget)
          .click(function(){
            var multiple_one_new = $( multiple_one_template({ index : widget.find('.multiple-one').length }) );
            widget.find('.multiple-list').append( multiple_one_new );
            my.initOne( multiple_one_new );
          });

      });

    },

    initOne : function(one) {

      var fieldset = $(one).find('fieldset');

      // reorganize layout

      var value_input_html = $(one)
        .find('[name*="@value"]')
        .addClass('form-control')
        .detach()[0].outerHTML;

      var current_language_code = $(one).find('[name*="@language"]').val();
      var button_text = current_language_code ? i18nStrings["iso639-1"][ current_language_code ] : 'Language';

      fieldset.append(
        my.templates['input-group']({
          'value_input_html' : value_input_html,
          'button_text' : button_text
        })
      );

      // set / unset @language based on @value

      $(one).find('[name*="@value"]').bind("input propertychange", function() {
        if ($(this).val() == "") {
          my.setLanguage(one, "");
        } else if ( $(one).find('[name*="@language"]').val() == "" ) {
          my.setLanguage(one, "en");
        }
      });

      // save controls

      var language_button = $(one).find('button');
      var dropdown = $(one).find('.dropdown-menu')
      var dropdown_parent = dropdown.parent();
      var language_input = $(one).find('[name*="@language"]');
      var typeahead = $(one).find('.typeahead');

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

      // focus typeahead on dropdown + hack to initially open the typeahead suggestion list

      dropdown_parent.on('shown.bs.dropdown', function(){
        typeahead.focus();
        typeahead.typeahead('val', 'x');
        typeahead.typeahead('val', '');
      });

      // when selection is made ...

      typeahead.bind('typeahead:select', function(e, suggestion) {
        language_button.dropdown('toggle');
        typeahead.typeahead('val', '');
        my.setLanguage(one, suggestion.id);
      });

    },

    setLanguage : function(multiple_one, language_code) {
      $( multiple_one ).find('[name*="@language"]').val( language_code );
      $( multiple_one ).find('.dropdown-toggle .text').text(
        language_code ? i18nStrings["iso639-1"][ language_code ] : 'Language'
      );
    },

    attached : []
  };

  Hijax.behaviours.localizedInput = my;
  return Hijax;

})(jQuery, Hijax);
