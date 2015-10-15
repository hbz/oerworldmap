function makeid() {
  var text = "";
  var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

  for( var i=0; i < 5; i++ )
    text += possible.charAt(Math.floor(Math.random() * possible.length));

  return text;
}

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
      $('input.filter', context).click(function() {
        $(this).closest("form").submit();
      });

      // hide empty filters
      $('form#search ul.filters:not(:has(*))', context).hide();

      // resource details table
      $('.resource-details-table', context).each(function(){
        $(this).find('.resource-list.truncated').each(function(){

          if(
            $(this).find('li').size() > 5
          ) {
            var id = "resource-list-collapsed-" + makeid();
            $(this).find('li:gt(4)').wrapAll('<div class="collapse" id="' + id + '"></div>');
            $(this).after('<a href="#' + id + '" class="resource-list-show-more collapsed" data-toggle="collapse"><span class="more">Show more <i class="fa fa-arrow-down"></i></span><span class="less">Show less <i class="fa fa-arrow-up"></i></span></a>');
          }

        });
      });

    }
  }

  Hijax.behaviours.other = my;
  return Hijax;

})(jQuery, Hijax);
