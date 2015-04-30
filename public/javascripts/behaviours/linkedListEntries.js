Hijax.behaviours.linkedListEntries = {
  
  attach : function(context) {
    var that = this;
    
    $('[data-behaviour="linkedListEntries"]', context).each(function(){
      $( this ).on("click", "li", function(){
        window.location = $( this ).find("h1 a").attr("href");
      });
    });
    
    $('[data-behaviour="linkedListEntries"]', context).each(function(){
      $( this ).on("mouseenter", "li", function(){
        $( Hijax.behaviours.map.container )
          .find('a[id="' + $( this ).find("h1 a").attr("href").split("/")[2] + '"] .placemark').attr("class","placemark active");
      });
      $( this ).on("mouseleave", "li", function(){
        $( Hijax.behaviours.map.container )
          .find('a[id="' + $( this ).find("h1 a").attr("href").split("/")[2] + '"] .placemark').attr("class","placemark");
      });
    });
  }
  
};