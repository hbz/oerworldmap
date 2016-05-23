/*

  column names: map, index, detail
  index modes: floating, list, statistic
  detail modes: expanded, collapsed, hidden

  TODO: check initialization_source to avoid dedundant requests on init

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

  var initialization_source = window.location;
  var initialization_content = document.documentElement.innerHTML;
  var map_and_index_source = '';
  var detail_source = '';

  function get(url, callback) {
    if(url == initialization_source.pathname + initialization_source.search) {
      callback(initialization_content);
    } else {
      $.ajax(url, {
        method : 'GET',
        success : callback
      });
    }
  }

  function get_main(data) {
    var body_mock = $(
      '<div id="body-mock">' +
      data.replace(/^[\s\S]*<body.*?>|<\/body>[\s\S]*$/ig, '') +
      '</div>'
    );
    return Hijax.attachBehaviours( body_mock.find('main') );
    // return body_mock.find('main').contents();
  }

  function set_map_and_index_source(url, index_mode) {
    if(url != map_and_index_source) {
      get(url, function(data){
        $('#app-col-index [data-app="col-content"]').html(
          get_main( data )
        );
        map_and_index_source = url;
      });
    }
    $('#app-col-index').attr('data-col-mode', index_mode);
  }

  function set_detail_source(url) {
    if(url != detail_source) {
      get(url, function(data){
        $('#app-col-detail [data-app="col-content"]').html(
          get_main( data )
        );
        detail_source = url;
      });
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
    if(pagejs_ctx.path != initialization_source.pathname) {
      window.location = pagejs_ctx.path;
    }
  }

  function route_login(pagejs_ctx) {
    $.get('/.login', function(){
      window.location = "/";
    });
  }

  function route_default(pagejs_ctx, next) {
    get(pagejs_ctx.path, function(data, textStatus, jqXHR){
      get_main( data );
    });
    next();
  }

  function routing_done(pagejs_ctx) {
    Hijax.layout();
  }

  var my = {

    init : function(context) {

      if(!init_app) {
        my.initialized.resolve();
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

      page('/', route_index, routing_done);
      page('/resource/', route_index, routing_done);
      page('/aggregation/', route_index, routing_done);
      page('/resource/:id', route_detail, routing_done);
      page('/country/:id', route_index_country, routing_done);

      // setup non-app (currently static) and login routes

      page(new RegExp('(' + static_pages.join('|').replace(/\//g, '\\\/') + ')'), route_static, routing_done);
      page('/.login', route_login, routing_done);

      // after all app routes ...

      page('*', route_default, routing_done);

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

      $('#app', context).on('click', '[data-app="to-modal-and-attach-behaviours"]', function(e){
        var id = this.hash.substring(1);
        var content = $( this.hash ).clone().prop('id', id + '-clone');
        var modal = $('#app-modal');
        modal.find('.modal-body').append( content );
        Hijax.attachBehaviours( $('#app-modal') );
        modal.modal('show');
        e.preventDefault();
        // e.stopPropagation();
      });

      // clear modal content when closed

      $('#app-modal').on('hidden.bs.modal', function(){
        $('#app-modal').find('.modal-body').empty();
      });

      // catch form submition inside modals and handle it async

      $('#app-modal').on('submit', 'form', function(e){
        e.preventDefault();

        var form = $(this);

        $.ajax({
          type : 'POST',
          url : form.attr('action'),
          data : form.serialize(),
          success : function(data, textStatus, jqXHR) {

            console.log('jqXHR', jqXHR);
            // console.log('status', jqXHR.status)
            console.log('getAllResponseHeaders', jqXHR.getAllResponseHeaders());

            var content_type = jqXHR.getResponseHeader('Content-Type');

            // in any case, get contents and attach behaviours
            if(content_type.indexOf("text/plain") > -1) {
              var contents = data;
            } else {
              console.log('get_main on post response');
              var contents = get_main( data );
            }

            // get the location header, because if a resource was successfully created the response is forwarding to it
            var location = jqXHR.getResponseHeader('Location');

            if(location) {

              // parse location and pass to router
              var just_a_parser = document.createElement('a');
              just_a_parser.href = location;
              page(just_a_parser.pathname);

            } else if(form.attr('action') == detail_source) {

              // if updated resource is currently lodaded in the detail column, update column and close modal
              $('#app-col-detail [data-app="col-content"]').html( contents );
              $('#app-modal').modal('hide');

            } else {

              $('#app-modal').find('.modal-body')
                .empty()
                .append( contents );

            }

          },
          error : function(jqXHR, textStatus, errorThrown){

            console.log('jqXHR', jqXHR);
            // console.log('status', jqXHR.status)
            console.log('getAllResponseHeaders', jqXHR.getAllResponseHeaders());

            var content_type = jqXHR.getResponseHeader('Content-Type'); console.log('content_type', content_type);

            // in any case, get contents and attach behaviours
            if(content_type.indexOf("text/plain") > -1) { console.log("text/plain");
              var contents = jqXHR.responseText;
            } else {
              var contents = get_main( jqXHR.responseText );
            }

            // if it's a play error replace everything with received data ... app will be gone
            if( jqXHR.responseText.indexOf('<body id="play-error-page">') > -1 ) {
              var new_doc = document.open("text/html", "replace");
              new_doc.write(jqXHR.responseText);
              new_doc.close();
            }

            if( form.find('.messages').length ) {
              form.find('.messages')
                .empty().append( contents )
                .addClass('active');
            } else {
              form.prepend( contents );
            }

          }
        });
      });

      // deferr
      my.initialized.resolve();
    },

    attach : function(context) {

      if(!init_app) {
        return;
      }

      $(context).find('[data-app="to-modal-on-load"]').each(function(){
        console.log("to-modal-on-load");
        var content = $( this ).children().clone();
        var modal = $('#app-modal');
        modal.find('.modal-body').empty().append( content );
        Hijax.attachBehaviours( $('#app-modal') );
        modal.modal('show');
      });

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

    },

    initialized : new $.Deferred()

  };

  Hijax.behaviours.app = my;

  return Hijax;

})(jQuery, Hijax, page);
