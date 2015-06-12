Hijax.behaviours.linkedListEntries = {

  attach : function(context) {
    var that = this;

    $('[data-behaviour="linkedListEntries"]', context).each(function(){
      $( this ).on("click", "li", function(){
        window.location = $( this ).find("h1 a").attr("href");
      });
    });

    $('[data-behaviour="linkedListEntries"]', context).each(function(){
      $( this ).on("mouseenter", "li", function() {
        var id = this.getAttribute("about");
        if (!id) {
          return;
        }
        Hijax.behaviours.map.world.getLayers().forEach(function(layer) {
          var markers = Hijax.behaviours.map.getFeaturesByReferencedId(layer, id);
          for (var i = 0; i < markers.length; i++) {
            var style = markers[i].getStyle();
            style.getText().setFont('normal 3em FontAwesome');
            markers[i].setStyle(style);
          }
        });
      });
      $( this ).on("mouseleave", "li", function(){
        var id = this.getAttribute("about");
        if (!id) {
          return;
        }
        Hijax.behaviours.map.world.getLayers().forEach(function(layer) {
          var markers = Hijax.behaviours.map.getFeaturesByReferencedId(layer, id);
          for (var i = 0; i < markers.length; i++) {
            var style = markers[i].getStyle();
            style.getText().setFont('normal 1.5em FontAwesome');
            markers[i].setStyle(style);
          }
        });
      });
    });
  }

};
