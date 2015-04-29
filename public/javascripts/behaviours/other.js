// --- other ---
Hijax.behaviours.other = {

  attach: function(context) {

    // placeholder polyfill
    $('input, textarea', context).placeholder();

    // collapse stories in aside
    $('article>header', context).siblings().hide();
    $('article:eq(0)>header', context).siblings().show();
    $('article>header', context).click(function() {
      $('article>header', context).siblings().slideUp(300);
      $(this).closest('article>header', context).siblings().slideToggle(300);
    });
    
    
    // call for actions
    $('a[href="#user-register"]', context).click(function(e){
      e.preventDefault();
      console.log($('#user-register'));
      $('#user-register').slideDown();
    });
    
  }

}
