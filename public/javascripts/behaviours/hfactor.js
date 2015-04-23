// --- hfactor ---
Hijax.behaviours.hfactor = {

  attach: function(context) {

    $('a.hijax.transclude', context).each(function() {
      var a = $(this);

      $.get(a.attr('href'))
        .done(function(data) {
          a.replaceWith(Hijax.attachBehaviours(Hijax.behaviours.hfactor.extractBody(data)));
          $('input, textarea').placeholder();
        })
        .fail(function(jqXHR) {
          a.replaceWith(Hijax.attachBehaviours(Hijax.behaviours.hfactor.extractBody(jqXHR.responseText)));
        });
    });

    $('form', context).submit(function() {
      var form = $(this);
      var loading_indicator = $(this).find('button[type="submit"] .loading-indicator');
      var action = form.attr('action');
      var method = form.attr('method');

      loading_indicator.show();

      $.ajax({
        type: method,
        url: action,
        data: form.serialize()
      }).done(function(data) {
        form.replaceWith(Hijax.attachBehaviours(Hijax.behaviours.hfactor.extractBody(data)));
        loading_indicator.hide();
      }).fail(function(jqXHR) {
        form.replaceWith(Hijax.attachBehaviours(Hijax.behaviours.hfactor.extractBody(jqXHR.responseText)));
        loading_indicator.hide();
      });

      return false;
    });

  },

  extractBody: function(html) {
    return $(html).filter('main').children();
  }
}
