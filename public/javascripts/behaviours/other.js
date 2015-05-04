// --- other ---
Hijax.behaviours.other = {

  attach: function(context) {

    // placeholder polyfill
    $('input, textarea', context).placeholder();

    // call for actions
    $('a[href="#user-register"]', context).click(function(e){
      e.preventDefault();
      console.log($('#user-register'));
      $('#user-register').slideDown();
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
