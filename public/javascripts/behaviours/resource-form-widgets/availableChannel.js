var Hijax = (function ($, Hijax) {

  var my = {

    templates : {},
    datasets : {},
    bloodhounds : {},
    bloodhoundDefers : {},

    attach : function(context) {

      // my.getBloodhound('/assets/json/esc.json');

      my.templates['widget'] = Handlebars.compile($('#availableChannel_widget\\.mustache').html());
      my.templates['item'] = Handlebars.compile($('#availableChannel_item\\.mustache').html());
      my.templates['multiple-one'] = Handlebars.compile($('#availableChannel_multiple-one\\.mustache').html());

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

      $('[data-behaviour~="availableChannel"]', context)
        .not('[data-dont-behave] [data-behaviour~="availableChannel"]')
        .each(function()
      {

        var widget = $(this);

        widget.addClass('behaving');

        widget.find('.multiple-one').last().remove();

        var serviceUrl_input = widget
          .find('[name*="[serviceUrl]"]')
          .addClass('form-control')
          .detach()[0].outerHTML;

        var language_list = widget
          .find('.multiple-list')
          .detach()[0].outerHTML;

        widget.append(
          $(my.templates['widget']({
            'serviceUrl_input' : serviceUrl_input,
            'language_list' : language_list
          }))
        );

        widget.find('.multiple-one').each(function(){
          my.initOne(this);
        });

        var key = widget.data('key');
        var title = widget.find('h4').text();

        var typeahead = widget.find('.typeahead');
        var dropdown = widget.find('.dropdown');
        var dropdown_button = dropdown.find('button');
        var dropdown_menu = dropdown.find('.dropdown-menu');

        dropdown_menu.on('click', function(){
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

        dropdown.on('shown.bs.dropdown', function(){
          typeahead.focus();
          typeahead.typeahead('val', 'x');
          typeahead.typeahead('val', '');
        });

        // when selection is made ...

        typeahead.bind('typeahead:select', function(e, suggestion) {
          dropdown_button.dropdown('toggle');
          typeahead.typeahead('val', '');

          if( widget.find('.multiple-one').length ) {
            var highest_index = parseInt(
              _(
                widget
                  .find('.multiple-one').last().find('input').attr('name')
                  .match(/\[(\d+)\]/)
              ).last()
            , 10);
          } else {
            var highest_index = 0;
          }

          var one_new = $(
            my.templates['multiple-one']({
              'key' : key,
              'index' : highest_index + 1,
              'language_code' : suggestion.id
            })
          );

          widget.find('.multiple-list').append( one_new );

          my.initOne( one_new );
        });

      });

    },

    initOne : function(one) {

      var language = i18nStrings.languages[ $(one).find('input').val() ]

      $(one).append(
        my.templates['item']({
          'language' : language
        })
      );

      $(one).find('[data-action="delete"]').click(function(){
        $(one).fadeOut(function(){
          $(this).remove();
        });
      });

    },

    attached : []
  };

  Hijax.behaviours.availableChannel = my;
  return Hijax;

})(jQuery, Hijax);
