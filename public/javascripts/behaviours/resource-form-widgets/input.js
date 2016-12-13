var Hijax = (function ($, Hijax) {

  var my = {

    attach : function(context) {

      // iterate over widgets

      $('[data-behaviour~="input"]', context)
        .not('[data-dont-behave] [data-behaviour~="input"]')
        .each(function()
      {

        var widget = $(this);

        $(this).addClass('behaving');

        if( widget.find('.multiple-list').length ) {

          // create fieldset template

          var multiple_one_html = $(widget.find('.multiple-one').last()[0].outerHTML);
          multiple_one_html.find('input').each(function() {
            this.setAttribute('name', this.getAttribute('name').replace(/\[\d+\]$/g, '[{{index}}]'));
            this.setAttribute('id', this.getAttribute('id').replace(/\[\d+\]$/g, '[{{index}}]'));
          });
          var multiple_one_template = Handlebars.compile(multiple_one_html[0].outerHTML);

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
              multiple_one_new.find('input').focus();
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

      var value_input = $(value_input_html).get(0);
      if (value_input.getAttribute('data-pattern')) {
        value_input.oninput = function() {
          this.setCustomValidity((!this.value || this.value.match(new RegExp(this.getAttribute('data-pattern'))) ? "" : "Invalid " + this.placeholder));
        };
      }

      $(one).append(value_input);

    }

  };

  Hijax.behaviours.input = my;
  return Hijax;

})(jQuery, Hijax);
