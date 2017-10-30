var Hijax = (function ($, Hijax) {

  var my = {

    templates : {},
    data_arrays : {},
    bloodhounds : {},

    attach : function(context) {

      my.templates['widget'] = Handlebars.compile($('#select_widget\\.mustache').html());
      my.templates['item'] = Handlebars.compile($('#select_item\\.mustache').html());
      my.templates['multiple-one'] = Handlebars.compile($('#select_multiple-one\\.mustache').html());

      // prepare iso3166-1-alpha-2 array

      my.data_arrays['iso3166-1-alpha-2'] = [];

      for(var i in i18nStrings["iso3166-1-alpha-2"]) {
        my.data_arrays['iso3166-1-alpha-2'].push({
          id: i,
          label: i18nStrings["iso3166-1-alpha-2"][i]
        });
      }

      my.data_arrays['iso3166-1-alpha-2'].sort(function(a,b) {
        if(a.label < b.label) return -1;
        if(a.label == b.label) return 0;
        if(a.label > b.label) return 1;
      });

      // init iso3166-1-alpha-2 bloodhound

      my.bloodhounds['iso3166-1-alpha-2'] = new Bloodhound({
        datumTokenizer: function(d){
          return Bloodhound.tokenizers.whitespace(
            bloodhoundAccentFolding.normalize(d.label)
          );
        },
        queryTokenizer: bloodhoundAccentFolding.queryTokenizer,
        local: my.data_arrays['iso3166-1-alpha-2'],
        identify: function(result){
          return result.id;
        },
      });

      // prepare iso639-1 array

      my.data_arrays['iso639-1'] = [];

      for(var i in i18nStrings["iso639-1"]) {
        my.data_arrays['iso639-1'].push({
          id: i,
          label: i18nStrings["iso639-1"][i]
        });
      }

      my.data_arrays['iso639-1'].sort(function(a,b) {
        if(a.label < b.label) return -1;
        if(a.label == b.label) return 0;
        if(a.label > b.label) return 1;
      });

      // init iso639-1 bloodhound

      my.bloodhounds['iso639-1'] = new Bloodhound({
        datumTokenizer: function(d){
          return Bloodhound.tokenizers.whitespace(
            bloodhoundAccentFolding.normalize(d.label)
          );
        },
        queryTokenizer: bloodhoundAccentFolding.queryTokenizer,
        local: my.data_arrays['iso639-1'],
        identify: function(result){
          return result.id;
        },
      });

      // iterate over widgets

      $('[data-behaviour~="select"]', context)
        .not('[data-dont-behave] [data-behaviour~="select"]')
        .each(function()
      {

        var widget = $(this);
        var key = widget.data('key');
        var options = widget.data('options');
        var title = widget.find('h4').text();

        $(this).addClass('behaving');

        widget.find('.multiple-one').last().remove();

        var list = widget
          .find('.multiple-list')
          .detach()[0].outerHTML;

        if(options == "iso3166-1-alpha-2") {
          var add_what = "Country";
        } else {
          var add_what = "availableLanguage";
        }

        widget.append(
          $(my.templates['widget']({
            'list' : list,
            'add_what' : add_what
          }))
        );

        // init each

        widget.find('.multiple-one').each(function(){
          my.initOne(this, options);
        });

        // grab what we need for dropdown

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
          name: options,
          limit: 9999,
          display: 'label',
          source: function(q, sync){
            if (q === '') {
              sync(my.data_arrays[ options ]);
            } else {
              my.bloodhounds[ options ].search(q, sync);
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
              'id' : suggestion.id
            })
          );

          widget.find('.multiple-list').append( one_new );

          my.initOne(one_new, options);
        });

      });

    },

    initOne : function(one, options) {

      var label = i18nStrings[ options ][ $(one).find('input').val() ];

      $(one).append(
        my.templates['item']({
          'item_label' : label
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

  Hijax.behaviours.select = my;
  return Hijax;

})(jQuery, Hijax);
