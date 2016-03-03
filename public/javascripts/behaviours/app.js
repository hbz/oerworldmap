var Hijax = (function ($, Hijax, page) {

  Hijax.goto = function(url) {
    page(url);
  };

  var templates = {
    'app' : Handlebars.compile($('#app\\.mustache').html())
  };

  var initialisation_source = '';
  var map_index_source = '';
  var detail_source = '';

  function set_map_index_source(url, index_mode) {
    if(url != map_index_source) {
      $.get(
        url,
        function(data){
          $('#app-col-index').html(
            Hijax.attachBehaviours( $(data).filter('main').contents() )
          )
          map_index_source = url;
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

  function route_index(pagejs_ctx){
    var index_mode;

    if( pagejs_ctx.pathname == "/" ) {
      index_mode = 'floating';
    }

    if( pagejs_ctx.pathname == "/resource/" ) {
      index_mode = 'list';
    }

    if( pagejs_ctx.pathname == "/aggregation/" ) {
      index_mode = 'aggregation';
    }

    var url = '/resource/' + (pagejs_ctx.querystring ? '?' + pagejs_ctx.querystring : '');
    set_map_index_source(url, index_mode);

    if(pagejs_ctx.hash) {
      set_detail_source('/resource/' + pagejs_ctx.hash);
    } else {
      $('#app-col-detail').attr('data-col-mode', 'hidden');
    }

  }

  function route_index_country(pagejs_ctx) {
    set_map_index_source(pagejs_ctx.path, 'list');
    $('#app-col-detail').attr('data-col-mode', 'hidden');

    if(pagejs_ctx.hash) {
      set_detail_source('/resource/' + pagejs_ctx.hash);
    } else {
      $('#app-col-detail').attr('data-col-mode', 'hidden');
    }
  }

  function route_detail(pagejs_ctx){
    if(
      /\/resource\/\?.*/.test(map_index_source) ||
      /\/country\/../.test(map_index_source)
    ) {
      page.redirect(map_index_source + '#' + pagejs_ctx.path.split('/')[2]);
    } else {
      set_map_index_source('/resource/', 'floating');
      set_detail_source(pagejs_ctx.path);
    }
  }

  Hijax.behaviours.hfactor = {

    init : function(context) {

      initialisation_source = window.location;

      $('body', context).append(
        templates.app({
          header : $('header', context)[0].outerHTML,
          footer : $('footer', context)[0].outerHTML
        })
      );

      page('/', route_index);
      page('/resource/', route_index);
      page('/resource/:id', route_detail);
      page('/country/:id', route_index_country);

      page();

      $('body>header, body>main, body>footer', context).remove();

      var deferred = new $.Deferred();
      deferred.resolve();
      return deferred;
    },

    attach : function(context) {

    }

  };

  return Hijax;

})(jQuery, Hijax, page);
