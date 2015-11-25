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

          var value_input = $(this)
            .find('[name*="@value"]')
            .addClass('form-control')
            .detach()[0].outerHTML;

          $(fieldset).append(
            templates['input-group']({
              'value_input' : value_input,
              'languages' : i18nStrings.countries
            })
          );

        });
      });

    }
  };

  Hijax.behaviours.localizedInput = my;
  return Hijax;

})(jQuery, Hijax);
