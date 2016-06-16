var Hijax = (function ($, Hijax) {

  var my = {
    
    attach: function(context) {

      $(window).resize(function(){
        my.scaleCharts();
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
