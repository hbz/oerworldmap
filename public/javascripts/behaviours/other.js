// --- other ---
Hijax.behaviours.other = {

  attach: function(context) {

    // placeholder polyfill
    $('input, textarea', context).placeholder();

    // call for actions
    $('a[href="#user-register"]', context).click(function(e){
      e.preventDefault();
      $(this).fadeOut();
      $('#user-register').slideDown();
    });
    
    $('[data-action="close"]', context).click(function(){
      e.preventDefault();
      $(this).parent().slideUp();
      $('a[href="#user-register"]', context).fadeIn();
    });

    // layout
    $('div[role="main"], div[role="complementary"]', context).each(function() {
      if (1 == $(this).index()) {
        $(this).css('left', '0');
        $(this).css('right', 'auto');
      } else {
        $(this).css('right', '0');
        $(this).css('left', 'auto');
      }
    });
    
  }

}
