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
      // $('input, textarea', context).placeholder();

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
          Hijax.goto( $( this ).find("h1 a").attr("href") );
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

      // sort ul
      $('ul[data-behaviour="sort"]', context).each(function() {
        var list = $(this);
        var listitems = list.children('li').get();
        listitems.sort(function(a, b) {
           return $(a).text().toUpperCase().localeCompare($(b).text().toUpperCase());
        })
        $.each(listitems, function(idx, itm) {
          list.append(itm);
        });
      });

      // bugfix for selects in modals see http://stackoverflow.com/questions/13649459/twitter-bootstrap-multiple-modal-error/15856139#15856139
      $.fn.modal.Constructor.prototype.enforceFocus = function () {};

      // prevent resource forms to submit on enter
      $('.resource-form-modal form').on("keypress", ":input:not(textarea)", function(event) {
        if (event.keyCode == 13) {
          event.preventDefault();
        }
      });

      // log out by clearing authentication cache or providing wrong credentials to apache
      // http://stackoverflow.com/a/32325848
      $('[data-behaviour="logout"]', context).click(function(event) {
        if (!document.execCommand("ClearAuthenticationCache")) {
          $.ajax({
            async: false,
            url: "/.login",
            type: 'GET',
            username: 'logout'
          });
        }
        window.location = "/";
        // event.preventDefault();
      });

      $('[data-behaviour="login"]', context).click(function(){
        var form = $(this).closest('form');
        var username = form.find('[name="email"]').val();
        var password = form.find('[name="password"]').val();

        $.ajax({
          async : false,
          url : "/.login",
          type : 'GET',
          username : username,
          password : password
        });

        window.location = "/";
      });

    }
  }

  Hijax.behaviours.other = my;
  return Hijax;

})(jQuery, Hijax);
