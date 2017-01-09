var Hijax = (function ($, Hijax) {

  var my = {

    attach: function(context) {

      $('svg').find('a').click(function(e) {
        Hijax.goto($(this).attr('xlink:href'));
        e.preventDefault();
      });

    },
    attached : []

  }

  Hijax.behaviours.charts = my;
  return Hijax;

})(jQuery, Hijax);
