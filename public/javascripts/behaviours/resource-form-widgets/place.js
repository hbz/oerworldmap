var Hijax = (function ($, Hijax) {

  var my = {

    templates : {},

    attach : function(context) {

      my.templates['widget'] = Handlebars.compile($('#place_widget\\.mustache').html());

      $('[data-behaviour="place"]', context).each(function() {

        var widget = $(this);

        widget.addClass('behaving');

        var inputs = {
          'location[address][streetAddress]' : null,
          'location[address][addressLocality]' : null,
          'location[address][postalCode]' : null,
          'location[address][addressCountry]' : null,
          'location[geo][lat]' : null,
          'location[geo][lon]' : null
        };

        var placeholders = {
          'location[address][streetAddress]' : 'Address',
          'location[address][addressLocality]' : 'City',
          'location[address][postalCode]' : 'ZIP',
          'location[address][addressCountry]' : 'Country',
          'location[geo][lat]' : 'Latitude',
          'location[geo][lon]' : 'Longitude'
        }

        for(name in inputs) {
          var dashed_name = name.replace('[', '-').replace('][', '-').replace(']', ''); // identifiers with [ don't work in handlebars
          inputs[ dashed_name ] = widget
            .find('[name$="' + name + '"]')
            .addClass('form-control')
            .attr('placeholder', placeholders[ name ])
            .detach()[0].outerHTML;
        }

        widget.append(
          $(my.templates['widget']( inputs ))
        );

      });

    }

  };

  Hijax.behaviours.place = my;
  return Hijax;

})(jQuery, Hijax);
