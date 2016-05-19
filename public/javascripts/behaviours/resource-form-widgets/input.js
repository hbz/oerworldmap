var Hijax = (function ($, Hijax) {

  var my = {

    attach : function(context) {

      // iterate over widgets

      $('[data-attach~="input"] [data-behaviour~="input"]', context).each(function() {

        var widget = $(this);

        $(this).addClass('behaving');

        if( widget.find('.multiple-list').length ) {

          // create fieldset template

          var multiple_one_template = Handlebars.compile(
            widget.find('.multiple-one').last()[0].outerHTML.replace(
              /\[\d+\]/g,
              '[{{index}}]'
            )
          );

          // if more than one, remove the last one

          if( widget.find('.multiple-one').length > 1 ) {
            widget.find('.multiple-one').last().remove();
          }

          // append add control

          $('<span class="small" data-action="add">+ Add ' + widget.find('.multiple-list').attr('title') + '</span>')
            .appendTo(widget)
            .click(function(){
              var multiple_one_new = $( multiple_one_template({ index : widget.find('.multiple-one').length }) );
              widget.find('.multiple-list').append( multiple_one_new );
              my.initOne( multiple_one_new );
            });

        }

        // init each

        widget.find('.multiple-one, .single').each(function(){
          my.initOne(this);
        });

      });

    },

    initOne : function(one) {

      var value_input_html = $(one)
        .find('input')
        .addClass('form-control')
        .detach()[0].outerHTML;

      $(one).append(value_input_html);

    }

  };

  Hijax.behaviours.input = my;
  return Hijax;

})(jQuery, Hijax);
