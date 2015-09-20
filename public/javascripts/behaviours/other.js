// --- other ---
var Hijax = (function ($, Hijax) {

  var my = {
    attach: function(context) {

      // placeholder polyfill
      $('input, textarea', context).placeholder();

      // call for actions
      $('a[href="#user-register"]', context).click(function(e){
        e.preventDefault();
        $(this).fadeOut();
        $('#user-register').slideDown();
      });

      $('[data-action="close"]', context).click(function(e){
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

      // clickable list entries
      $('[data-behaviour="linkedListEntries"]', context).each(function(){
        $( this ).on("click", "li", function(){
          window.location = $( this ).find("h1 a").attr("href");
        });
      });

      // trigger search on facet select
      $('form#search input.filter', context).click(function() {
        $('form#search').submit();
      });

      // hide empty filters
      $('form#search ul.filters:not(:has(*))', context).hide();

    }
  }

  Hijax.behaviours.other = my;
  return Hijax;

})(jQuery, Hijax);
