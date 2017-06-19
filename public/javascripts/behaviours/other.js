function makeid() {
  var text = "";
  var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

  for( var i=0; i < 5; i++ )
    text += possible.charAt(Math.floor(Math.random() * possible.length));

  return text;
}

// --- other ---

var Hijax = (function ($, Hijax) {

  // log out by clearing authentication cache or providing wrong credentials to apache
  // http://stackoverflow.com/a/32325848

  function logout() {
      if (!document.execCommand("ClearAuthenticationCache")) {
        $.ajax({
          async: false,
          url: "/.login",
          type: 'GET',
          username: 'logout'
        });
      }
      window.location = "/";
  }

  var my = {
    attach: function(context) {

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

      $('[data-behaviour~="logout"]', context)
        .not('[data-dont-behave] [data-behaviour~="logout"]')
        .click( logout );

      $('[data-behaviour~="logout-on-load"]', context)
        .not('[data-dont-behave] [data-behaviour~="logout-on-load"]')
        .each(function()
      {
        setTimeout(function(){
          logout();
        }, 5000);
      });

      $('[data-behaviour~="login"]', context)
        .not('[data-dont-behave] [data-behaviour~="login"]')
        .click(function()
      {
        var form = $(this).closest('form');
        var username = form.find('[name="email"]').val();
        var password = form.find('[name="password"]').val();

/*
        $.ajax({
          async : false,
          url : "/.login",
          type : 'GET',
          username : username,
          password : password
        });
*/

        $.ajax({
          async : false,
          url : "/.login",
          type : 'GET',
          xhrFields : {
            withCredentials : true
          },
          beforeSend : function(xhr) {
            xhr.setRequestHeader('Authorization', 'Basic ' + btoa(username + ':' + password));
          },
          error : function(jqXHR) {
            alert('Wrong username or password.');
            jqXHR.abort();
            // window.location = "/";
          },
          success : function() {
            window.location = "/";
          }
        });

      });

      // global statistics
/*
      $('[data-behaviour~="global-statistic"]', context).slice(1).hide();
      $('[data-behaviour~="global-statistic-switch"]', context).click(function(e) {
        var id = $(this).attr("href").slice(1);
        $('[data-behaviour~="global-statistic"]', context).hide();
        $("[id='" + id + "']", context).show();
        e.preventDefault();
      });
*/


      $('[data-behaviour~="statistic"]', context)
        .not('[data-dont-behave] [data-behaviour~="statistic"]')
        .each(function()
      {

        var n = $(this).find('table tr').length;

        log.info('found statistic', n);

        var scale = d3.scale.linear()
  		    .domain([0, (n-1)/2, (n-1)])
          .range([
            '#a0c846',
            '#ffe600',
            '#ff8c00'
          ]);

        for(var i = 0; i < n; i++) {
          //log.info('coloring', $(this).find('div.color-' + i));
          $(this).find('div.color-' + i).css({
            background : scale(i)
          });
          $(this).find('path.color-' + i).css({
            fill : scale(i)
          });
        }

		  });

      // carousel
      $('[data-behaviour~="carousel"]', context)
        .not('[data-dont-behave] [data-behaviour~="carousel"]')
        .each(function()
      {
        var children = $(this).children();
        if (children.length > 1) {
          var i = 0;
          children.hide();
          $(children.get(i)).show();
          setInterval(function() {
            $(children.get(i)).fadeOut(400, function() {
              i = (i + 1) % children.length;
              $(children.get(i)).fadeIn(400);
            });
          }, 5000);
        }
      });


      $('[data-behaviour~="delete-resource"]', context)
        .not('[data-dont-behave] [data-behaviour~="delete-resource"]')
        .bind('click', function(e)
      {
        if (window.confirm("Delete resource?")) {
          $.ajax({
            url : this.getAttribute('href'),
            type : 'DELETE',
            success : function() {
              alert('Resource deleted.');
              window.location = window.location.hash ? window.location.pathname + window.location.search : '/';
            },
            error : function(jqXHR) {
              alert('An error has occurred.');
            }
          });
        }
        return false;
      });

      $('[data-behaviour~="select-on-click"]', context)
        .not('[data-dont-behave] [data-behaviour~="select-on-click"]')
        .bind('click', function(e)
      {
        this.select();
      });

      $('[data-behaviour~="print-index"]', context)
        .not('[data-dont-behave] [data-behaviour~="print-index"]')
        .bind('click', function(e)
      {
        $('#app').addClass('print-index');
        window.print();
        $('#app').removeClass('print-index');
      });

      $('[data-behaviour~="continue-link"]', context)
        .not('[data-dont-behave] [data-behaviour~="continue-link"]')
        .each(function()
      {
        $(this).attr("href", $(this).attr("href") + "?continue=" + encodeURIComponent($('#app-modal').data('url_before')));
      });

      /* --- collapsed form field --- */

      $('[data-behaviour~="collapsed-form-field"] button', context)
        .not('[data-dont-behave] [data-behaviour~="print-index"]')
        .bind('click', function(e)
      {
        var $el = $(this).closest('[data-behaviour~="collapsed-form-field"]');
        $el.addClass('in');
        Hijax.attachBehaviours($el.find('.collapse'), 'triggered by collapsed-form-field');
      });

      /* --- show link to external image sources --- */

      $('.paragraph-image img[src^="http"], .html-from-markdown img[src^="http"]', context).each(function(){
        $(this).after('<div class="image-source"><a href="' + this.src + '" target="_blank">(Image Source)</a></div>')
      });

    },

    attached : []
  };

  Hijax.behaviours.other = my;
  return Hijax;

})(jQuery, Hijax);
