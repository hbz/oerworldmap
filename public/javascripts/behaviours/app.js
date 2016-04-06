/*

  column names: map, index, detail
  index modes: floating, list, statistic
  detail modes: expanded, collapsed, hidden

  TODO: check initialisation_source to avoid dedundant requests on init

*/

var Hijax = (function ($, Hijax, page) {

  var static_pages = ["/contribute", "/FAQ", "/about"];

  var init_app = true;

  if( $.inArray(window.location.pathname, static_pages) !== -1) {
    init_app = false;
  };

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
          $('#app-col-index [data-app="col-content"]').html(
            Hijax.attachBehaviours( $(data).filter('main').contents() )
          );
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
          $('#app-col-detail [data-app="col-content"]').html(
            Hijax.attachBehaviours( $(data).filter('main').contents() )
          );
          detail_source = url;
        }
      );
    }
    // preserve collapsed state / avoid auto expanding of collapsed detail when switching from floating to list
    if($('#app-col-detail').attr('data-col-mode') != 'collapsed') {
      $('#app-col-detail').attr('data-col-mode', 'expanded');
    }
  }

  function route_index(pagejs_ctx, next) {
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

  function route_detail(pagejs_ctx, next) {
    set_map_and_index_source('/resource/', 'floating');
    set_detail_source(pagejs_ctx.path);

    next();
  }

  function route_static(pagejs_ctx) {
    if(pagejs_ctx.path != initialisation_source.pathname) {
      window.location = pagejs_ctx.path;
    }
  }

  function routing_done() {
    Hijax.layout();
  }

  Hijax.behaviours.hfactor = {

    init : function(context) {

      if(!init_app) {
        var deferred = new $.Deferred();
        deferred.resolve();
        return deferred;
      }

      // render template / cleanup dom

      $('body', context).append(
        templates.app({
          header : $('header', context)[0].outerHTML,
          footer : $('footer', context)[0].outerHTML
        })
      );

      $('body>header, body>main, body>footer', context).remove();

      $('footer', context).removeClass(); // to remove default (non app) styling

      // setup app routes

      page('/', route_index);
      page('/resource/', route_index);
      page('/aggregation/', route_index);
      page('/resource/:id', route_detail);
      page('/country/:id', route_index_country);

      // setup non-app (currently static) routes

      page(new RegExp('(' + static_pages.join('|').replace(/\//g, '\\\/') + ')'), route_static);

      // after all app routes ...

      page('*', routing_done);

      // start routing

      page();

      // bind index switch

      $('#app', context).on('click', '[data-app-index-switch] a', function(e) {
        var hash = this.href.split('#')[1];
        if( hash == 'list' ) {
          page('/resource/' + window.location.search + window.location.hash);
        } else if( hash == 'statistic' ) {
          page('/aggregation/' + window.location.search);
        }
        e.preventDefault();
      });

      // bind column toggle

      $('#app', context).on('click', '[data-app="toggle-col"]', function(e) {
        var col = $(this).closest('[data-app="col"]');
        if(col.is('#app-col-index')) {

          if(window.location.search) {
            page('/' + window.location.search + window.location.hash);
          } else if(window.location.hash) {
            page('/resource/' + window.location.hash.split('#')[1]);
          } else {
            page('/');
          }

        } else if(col.is('#app-col-detail') && col.attr('data-col-mode') == 'expanded') {
          col.attr('data-col-mode', 'collapsed');
        } else if(col.is('#app-col-detail') && col.attr('data-col-mode') == 'collapsed') {
          col.attr('data-col-mode', 'expanded');
        }
        Hijax.layout();
      });

      // catch links to fragments

      $('#app', context).on('click', '[data-app="link-to-fragment"] a', function(e){
        page(map_and_index_source + '#' + this.href.split('/').pop());
        e.preventDefault();
        e.stopPropagation();
      });

      // bind modal links

      $('#app', context).on('click', '[data-app="to-modal"]', function(e){
        var content = $( this.hash )
          .after('<div data-placeholder="' + this.hash + '"></div>')
          .detach();
        var modal = $('#app-modal');
        modal.find('.modal-body').append(content);
        modal.data('content', this.hash);
        modal.modal('show');
        e.preventDefault();
        // e.stopPropagation();
      });

      // move modal content back to placeholder, when modal is closed

      $('#app-modal').on('hidden.bs.modal', function(){
        var modal = $(this);
        $('[data-placeholder="' + modal.data('content') + '"]').replaceWith(
          modal.find('.modal-body').contents()
        );
        modal.data('content', '');
      });

      // catch form submition inside modals and handel it async

      $('#app-modal').on('submit', 'form', function(e){
        e.preventDefault();
        var form = $(this);

        $.ajax({
          type : 'POST',
          url : form.attr('action'),
          data : form.serialize(),
          success : function(data, textStatus, jqXHR){
            console.log(jqXHR);
            console.log(jqXHR.getAllResponseHeaders());

            parser = document.createElement('a');
            parser.href = jqXHR.getResponseHeader('Location');
            page(parser.pathname);
            $('#app-modal').modal('hide');
          },
          error : function(jqXHR, textStatus, errorThrown){
            console.log(jqXHR);
            console.log(jqXHR.getAllResponseHeaders());

            $('#' + form.attr('target')).append(
              $(jqXHR.responseText).find('#messages')
            );
          }
        });
      });

      // deferr

      var deferred = new $.Deferred();
      deferred.resolve();
      return deferred;
    },

    attach : function(context) {

      if(!init_app) {
        return;
      }

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
