/*

  column names: map, index, detail
  index modes: floating, list, statistic
  detail modes: expanded, collapsed, hidden

  TODO: check initialisation_source to avoid dedundant requests on init

*/

var Hijax = (function ($, Hijax, page) {

  Hijax.goto = function(url) {
    page(url);
  };

  var templates = {
    'app' : Handlebars.compile($('#app\\.mustache').html())
  };

  var initialisation_source = window.location;
  var map_and_index_source = '';
  var detail_source = '';

  function set_map_and_index_source(url, index_mode) {
    if(url != map_and_index_source) {
      $.get(
        url,
        function(data){
          $('#app-col-index').html(
            Hijax.attachBehaviours( $(data).filter('main').contents() )
          )
          map_and_index_source = url;
        }
      )
    }
    $('#app-col-index').attr('data-col-mode', index_mode);
  }

  function set_detail_source(url) {
    if(url != detail_source) {
      $.get(
        url,
        function(data){
          $('#app-col-detail').html(
            Hijax.attachBehaviours( $(data).filter('main').contents() )
          )
          detail_source = url;
        }
      );
    }
    $('#app-col-detail').attr('data-col-mode', 'expanded');
  }

  function route_index(pagejs_ctx, next){
    var index_mode;

    if( pagejs_ctx.pathname == "/" ) {
      index_mode = 'floating';
    }

    if( pagejs_ctx.pathname == "/resource/" ) {
      index_mode = 'list';
    }

    if( pagejs_ctx.pathname == "/aggregation/" ) {
      index_mode = 'statistic';
    }

    var url = '/resource/' + (pagejs_ctx.querystring ? '?' + pagejs_ctx.querystring : '');
    set_map_and_index_source(url, index_mode);

    if(pagejs_ctx.hash) {
      set_detail_source('/resource/' + pagejs_ctx.hash);
    } else {
      $('#app-col-detail').attr('data-col-mode', 'hidden');
    }

    next();
  }

  function route_index_country(pagejs_ctx, next) {
    set_map_and_index_source(pagejs_ctx.path, 'list');
    $('#app-col-detail').attr('data-col-mode', 'hidden');

    if(pagejs_ctx.hash) {
      set_detail_source('/resource/' + pagejs_ctx.hash);
    } else {
      $('#app-col-detail').attr('data-col-mode', 'hidden');
    }

    next();
  }

  function route_detail(pagejs_ctx, next){
    /*
      check if database for map and index are filtered
      if so, redirect to current url and append id as fragment identifier
      if not, set map and index to unfiltered and load detail
    */
    if(
      /\/resource\/\?.*/.test(map_and_index_source) ||
      /\/country\/../.test(map_and_index_source)
    ) {
      page.redirect(map_and_index_source + '#' + pagejs_ctx.path.split('/')[2]);
    } else {
      set_map_and_index_source('/resource/', 'floating');
      set_detail_source(pagejs_ctx.path);
    }

    next();
  }

/*
  function routing_before(pagejs_ctx, next) {
    console.log(pagejs_ctx);
    if( pagejs_ctx.hash == "statistic" ) {
      page.redirect('/aggregation/' + (pagejs_ctx.querystring ? '?' + pagejs_ctx.querystring : ''));
    } else if( pagejs_ctx.hash == "list" ) {
      page.redirect('/resource/' + (pagejs_ctx.querystring ? '?' + pagejs_ctx.querystring : ''));
    } else {
      next();
    }
  }
*/

  function routing_done() {
    Hijax.layout();
  }

  Hijax.behaviours.hfactor = {

    init : function(context) {

      $('body', context).append(
        templates.app({
          header : $('header', context)[0].outerHTML,
          footer : $('footer', context)[0].outerHTML
        })
      );

      // page('*', routing_before);
      page('/', route_index);
      page('/resource/', route_index);
      page('/aggregation/', route_index);
      page('/resource/:id', route_detail);
      page('/country/:id', route_index_country);
      page('*', routing_done);

      page();

      $('body>header, body>main, body>footer', context).remove();

      $('#app', context).on('click', '[data-app-index-switch] a', function(e) {
        var hash = this.href.split('#')[1];
        if( hash == 'list' ) {
          page('/resource/' + window.location.search);
        } else if( hash == 'statistic' ) {
          page('/aggregation/' + window.location.search);
        }
        e.preventDefault();
      })

      var deferred = new $.Deferred();
      deferred.resolve();
      return deferred;
    },

    attach : function(context) {

      $('#app', context).on('submit', 'form', function() {
        var form = $(this);
        if (
          ! form.attr('method') ||
          form.attr('method').toUpperCase() == 'GET'
        ) {
          var action = form.attr("action") || '';
          page(action + "?" + form.serialize());
          return false;
        }
      });

    }

  };

  return Hijax;

})(jQuery, Hijax, page);
