// --- hfactor ---
Hijax.behaviours.hfactor = {

  attach: function(context) {

    $('a.hijax[target]', context).each(function() {

      var a = $(this);

      a.bind('click', function() {
        $.get(a.attr('href'))
          .done(function(data) {
            Hijax.behaviours.hfactor.append(data, a);
          })
          .fail(function(jqXHR) {
            Hijax.behaviours.hfactor.append(jqXHR.responseText, a);
          });
        return false;
      });

      if (a.hasClass('transclude')) {
        a.trigger('click');
      }

    });

    $('form.hijax[target]', context).submit(function() {

      var form = $(this);
      var action = form.attr('action');
      var method = form.attr('method');

      $.ajax({
        type: method,
        url: action,
        data: form.serialize()
      }).done(function(data) {
        Hijax.behaviours.hfactor.append(data, form);
      }).fail(function(jqXHR) {
        Hijax.behaviours.hfactor.append(jqXHR.responseText, form);
      });

      return false;

    });

  },

  extractBody: function(html) {
    return $(html).filter('div[role="main"]');
  },

  append: function(data, element) {

    var role = element.attr('role') || 'complementary';
    var parent = element.closest('div[role="main"], div[role="complementary"]');
    var html = Hijax.attachBehaviours(Hijax.behaviours.hfactor.extractBody(data));

    if ('main' == role) {
      element.closest('body').children('div[role="main"]').attr("role", "complementary");
    }

    switch (element.attr("target")) {
      case "_self":
        element.replaceWith(html.children());
        break;
      case "_blank":
        html.attr("role", role);
        parent.after(html);
        break;
      case "_parent":
      default:
        html.attr("role", role);
        parent.replaceWith(html);
        break;
    }

  }

};
