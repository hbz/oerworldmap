(function ($, document) {

  // --- set js class on <html>

  document.documentElement.className = 'js';

  // --- document ready ---

  $(document).ready(function() {
    
    var page = window.location.pathname.substring(1);
    
    if(
      page != "contribute" &&
      page != "FAQ" &&
      page != "about" &&
      page != "contribute" &&
      page != "imprint"
    ) {
      $('body').addClass("layout-fixed");
    }
    
    Hijax.initBehaviours(document);

  });

})(jQuery, document);
