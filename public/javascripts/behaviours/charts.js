var Hijax = (function ($, Hijax) {

  var my = {

    attach: function(context) {

      $(window).resize(function(){
        my.scaleCharts();
      });

      $('svg').find('a').click(function(e) {
        Hijax.goto($(this).attr('xlink:href'));
        e.preventDefault();
      });

    },

    layout: function() {
      my.scaleCharts();
    },

    scaleCharts: function() {

      $('svg.chart').each(function(){
        $(this).css({
    			width: $(this).parent().width(),
    			height: $(this).parent().width() * ( this.getAttribute("width") / this.getAttribute("height") )
    		});
      });

    }

  }

  Hijax.behaviours.charts = my;
  return Hijax;

})(jQuery, Hijax);
