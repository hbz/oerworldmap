(function ($, document) {

  // --- set js class on <html>

  document.documentElement.className = 'js';

  // --- document ready ---

  $(document).ready(function() {

    $('body').addClass("layout-fixed");
    Hijax.initBehaviours(document);

  });

})(jQuery, document);
