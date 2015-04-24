// --- other ---
Hijax.behaviours.other = {

  attach: function(context) {

    // placeholder polyfill
    $('input, textarea', context).placeholder();


    // animate about section
    $('div#about ul>li', context).addClass("invisible");
    $('div#about', context).viewportChecker({
      offset: 80,
      callbackFunction: function() {
        $('div#about li', context).each(function(i) {
          var li = this;
          setTimeout(function() {
            $(li).addClass("visible").addClass("animated").addClass("fadeInUp");
          }, i * 1000);
        });
      }
    });

    // collapse stories in aside
    $('article>header', context).siblings().hide();
    $('article:eq(0)>header', context).siblings().show();
    $('article>header', context).click(function() {
      $('article>header', context).siblings().slideUp(300);
      $(this).closest('article>header', context).siblings().slideToggle(300);
    });

  }

}
