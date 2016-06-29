var Hijax = (function ($, Hijax) {

  var my = {

    templates : {},

    attach : function(context) {

      my.templates['widget'] = Handlebars.compile($('#place_widget\\.mustache').html());

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

      $('[data-attach~="place"] [data-behaviour~="place"]', context).each(function() {

        var widget = $(this);

        widget.addClass('behaving');

        var inputs = {
          'location[address][streetAddress]' : null,
          'location[address][addressLocality]' : null,
          'location[address][postalCode]' : null,
          'location[address][addressRegion]' : null,
          'location[address][addressCountry]' : null,
          'location[geo][lat]' : null,
          'location[geo][lon]' : null
        };

        var placeholders = {
          'location[address][streetAddress]' : 'Address',
          'location[address][addressLocality]' : 'City',
          'location[address][postalCode]' : 'ZIP',
          'location[address][addressRegion]' : 'Province',
          'location[address][addressCountry]' : 'Country Code (DE, US, GB, ... to be replaced by dropdown)',
          'location[geo][lat]' : 'Latitude',
          'location[geo][lon]' : 'Longitude'
        }

        for(name in inputs) {
          var dashed_name = name.replace('[', '-').replace('][', '-').replace(']', ''); // identifiers with [ don't work in handlebars
          inputs[ dashed_name ] = widget
            .find('[name$="' + name + '"]')
            .addClass('form-control')
            .attr('placeholder', placeholders[ name ])
            .attr('title', placeholders[ name ])
            .detach()[0].outerHTML;
        }

        // fill widget template

        widget.append(
          $(my.templates['widget']( inputs ))
        );

        // init country typeahead

        var typeahead = widget.find('.typeahead');
        var dropdown = widget.find('.dropdown');
        var dropdown_button = dropdown.find('button');
        var dropdown_menu = dropdown.find('.dropdown-menu');

        var current_country_code = widget.find('[name="location[address][addressCountry]"]').val();

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
          widget.find('[name="location[address][addressCountry]"]').val(
            suggestion.id
          );
          dropdown_button.find('.text').text(
            suggestion.label
          );
        });

      });

    }

  };

  Hijax.behaviours.place = my;
  return Hijax;

})(jQuery, Hijax);
