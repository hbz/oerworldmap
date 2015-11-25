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

          //$(this).find('[name*="@value"]').remove();
          console.log(value_input);

          $(fieldset).append(
            templates['input-group']({
              'value_input' : value_input,
              'languages' : i18nStrings.countries
            })
          );

          var select2 = $(fieldset).find('select').select2({
            placeholder : "Language",
            theme : "bootstrap"
/*
            containerCssClass : "btn btn-default",
            templateSelection : function(){
              return $('<button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">Language <span class="caret"></span></button>');
            }
*/
          });


          select2.on("select2:open", function(e){

            // correct dropdown position to span full input group
/*
            $('.select2-dropdown').css({
              width: $(fieldset).width(),
            });
*/
            $('body > .select2-container').css({
              left : $(fieldset).offset().left
            })

            // add color scheme, because out of context
            $('body > .select2-container').addClass('color-scheme-text');
          });




/*
          $(fieldset).find('button').click(function(){
            $(fieldset).find('.language-select').show();
            select2.select2('open');
          });
*/
/*
          $(this).find('[name*="@value"]')
            .wrap('<div class="input-group"></div>')
            .after();
*/
        });
      });

    }
  };

  Hijax.behaviours.localizedInput = my;
  return Hijax;

})(jQuery, Hijax);
