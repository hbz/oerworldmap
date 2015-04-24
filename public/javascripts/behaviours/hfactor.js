// --- hfactor ---
Hijax.behaviours.hfactor = {

  attach: function(context) {

    $('a.hijax.transclude[target]', context).each(function() {

      var a = $(this);

      $.get(a.attr('href'))
        .done(function(data) {
          Hijax.behaviours.hfactor.append(data, a, context);
        })
        .fail(function(jqXHR) {
          Hijax.behaviours.hfactor.append(jqXHR.responseText, a, context);
        });

    });

    $('form[target]', context).submit(function() {

      var form = $(this);
      var loading_indicator = $(this).find('button[type="submit"] .loading-indicator');
      var action = form.attr('action');
      var method = form.attr('method');

      $.ajax({
        type: method,
        url: action,
        data: form.serialize()
      }).done(function(data) {
        Hijax.behaviours.hfactor.append(data, form, context);
      }).fail(function(jqXHR) {
        Hijax.behaviours.hfactor.append(jqXHR.responseText, form, context);
      });

      return false;

    });

  },

  extractBody: function(html) {
    return $(html).filter('main').children();
  },

  append: function(data, element, context) {
    switch (element.attr("target")) {
      case "_parent":
        $('main', context).after($('<aside />')
            .append(Hijax.attachBehaviours(Hijax.behaviours.hfactor.extractBody(data))));
        element.remove();
        break;
      case "_self":
        element.replaceWith(Hijax.attachBehaviours(Hijax.behaviours.hfactor.extractBody(data)));
        break;
    }
  }

};
