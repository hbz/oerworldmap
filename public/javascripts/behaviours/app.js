/*

  column names: map, index, detail
  index modes: floating, list, statistic
  detail modes: expanded, collapsed, hidden

  TODO: check initialization_source to avoid dedundant requests on init

*/

var Hijax = (function ($, Hijax, page) {

  var static_pages = ["/contribute", "/FAQ", "/about", "/imprint", "/log"];

  var init_app = true;

  $.each(static_pages, function() {
    if (window.location.pathname.indexOf(this) == 0) {
      init_app = false;
      return false;
    }
  });

  Hijax.goto = function(url) {
    page(url);
  };

  var templates = {
    'app' : Handlebars.compile($('#app\\.mustache').html()),
    'http_error' : Handlebars.compile($('#http_error\\.mustache').html())
  };

  var initialization_source = {
    pathname : window.location.pathname,
    search : window.location.search
  };
  var initialization_content = document.documentElement.innerHTML;

  var map_and_index_source = '';
  var detail_source = '';

  var map_and_index_loaded = $.Deferred().resolve();
  var detail_loaded = $.Deferred().resolve();

  map_and_index_loaded.msg = 'map_and_index_loaded';
  detail_loaded.msg = 'detail_loaded';

  var app_history = [];

  function get(url, callback, callback_error) {
    log.debug('APP get:', url);
    if(url == initialization_source.pathname + initialization_source.search) {
      log.debug('APP ... which is the initialization_content');
      callback(initialization_content);
    } else {
      log.debug('APP ... which needs to be ajaxed');
      $.ajax(url, {
        method : 'GET',
        success : callback,
        error : function(jqXHR) {
          to_modal(templates['http_error']({
            url : url,
            error : jqXHR.status + ' / ' + jqXHR.responseText
          }), 'load');
          if(typeof callback_error == 'function') {
            callback_error();
          }
        }
      });
    }
  }

  function get_main(data, url) {
    log.debug('APP get_main (and attach behaviours)', url);
    document.title = $(data).filter('title').text();
    // http://stackoverflow.com/a/12848798/1060128
    var body_mock = $(
      '<div id="body-mock">' +
      data.replace(/^[\s\S]*<body.*?>|<\/body>[\s\S]*$/ig, '') +
      '</div>'
    );
    return Hijax.attachBehaviours(body_mock.find('main'), 'triggered for ' + url);
  }

  function set_col_mode(col, mode) {
    $('#app-col-' + col).attr('data-col-mode', mode);
    $('#app').attr('data-col-' + col + '-mode', mode);
  }

  function set_map_and_index_source(url, index_mode) {
    log.debug('APP set_map_and_index_source', url);

    if(url != map_and_index_source) {
      map_and_index_loaded = $.Deferred();
      map_and_index_loaded.msg = 'map_and_index_loaded';
      get(url, function(data){
        $('#app-col-index [data-app="col-content"]').html(
          get_main(data, url)
        );
        map_and_index_source = url;
        Hijax.behaviours.map.setPlacemarksVectorSource(url);
        map_and_index_loaded.resolve();
      }, function(){
        map_and_index_loaded.resolve();
      });
    }
    set_col_mode('index', index_mode);
  }

  function set_detail_source(url) {
    log.debug('APP set_detail_source', url);

    if(url != detail_source) {
      detail_loaded = $.Deferred();
      get(url, function(data){
        $('#app-col-detail [data-app="col-content"]').html(
          get_main(data, url)
        );
        set_col_mode('detail', 'expanded');
        detail_source = url;
        detail_loaded.resolve();
      }, function(){
        detail_loaded.resolve();
      });
    } else {
      set_col_mode('detail', 'expanded');
      // to reset hightlighted pins, reattach map behaviour to detail ...
      var attached = new $.Deferred();
      Hijax.behaviours.map.attach($('#app-col-detail [data-app="col-content"]'), attached);
    }

    // set focus to fit highlighted
    // therefor waiting for map_and_index_loaded and map attachments

    var map_attached = new $.Deferred();
    $.when.apply(null, Hijax.behaviours.map.attached).done(function(){
      map_attached.resolve();
    });
  }

  function route_start(pagejs_ctx, next) {
    log.debug('APP route_start', pagejs_ctx);

    app_history.push(_(pagejs_ctx).clone());

    if(
      app_history.length > 1 &&
      pagejs_ctx.path == app_history[app_history.length - 2].path
    ) {
      pagejs_ctx.native_fragment = true;
    } else {
      pagejs_ctx.native_fragment = false;
    }

    var modal = $('#app-modal');
    if(
      (modal.data('bs.modal') || {}).isShown &&
      modal.data('url') != pagejs_ctx.path
    ) {
      modal.data('hidden_on_exit', true);
      modal.modal('hide');
    }

    next();
  }

  function route_index(pagejs_ctx, next) {
    log.debug('APP route_index', pagejs_ctx);

    $('#app').addClass('loading');

    // clear map highlights
    Hijax.behaviours.map.setHighlights([]);

    // clear empty searches

    if(pagejs_ctx.querystring == "q=") {
      page.redirect('/');
    }

    // trigger behaviour attachment for landing page

    if(
      pagejs_ctx.path == "/"
    ) {
      get('/', function(data){
        log.debug('APP getting main from:', '/')
        get_main(data, '/');
      });
    }

    // set map view change

    if(app_history.length == 1) {
      if(pagejs_ctx.path == "/") {
        Hijax.behaviours.map.scheduleViewChange('world');
      } else {
        Hijax.behaviours.map.scheduleViewChange('placemarks');
      }
    }
    Hijax.behaviours.map.setScope('world');

    // determine index_mode

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

    // set map and index source

    var url = '/resource/' + (pagejs_ctx.querystring ? '?' + pagejs_ctx.querystring : '');
    set_map_and_index_source(url, index_mode);

    // set detail source

    if(pagejs_ctx.hash) {
      set_detail_source('/resource/' + pagejs_ctx.hash);
    } else {
      set_col_mode('detail', 'hidden');
    }

    next();
  }

  function route_index_country(pagejs_ctx, next) {
    log.debug('APP route_index_country', pagejs_ctx);

    $('#app').addClass('loading');

    // clear map highlights
    Hijax.behaviours.map.setHighlights([]);

    set_map_and_index_source(pagejs_ctx.path, 'list');
    set_col_mode('detail', 'hidden');

    // set map scope
    Hijax.behaviours.map.setScope( pagejs_ctx.path.split("/").pop().toUpperCase() );

    if(pagejs_ctx.hash) {
      set_detail_source('/resource/' + pagejs_ctx.hash);
      if(app_history.length == 1) {
        Hijax.behaviours.map.scheduleViewChange('country');
      }
    } else {
      Hijax.behaviours.map.scheduleViewChange('country');
      set_col_mode('detail', 'hidden');
    }

    next();
  }

  function route_detail(pagejs_ctx, next) {
    log.debug('APP route_detail', pagejs_ctx);

    $('#app').addClass('loading');

    // clear map highlights
    Hijax.behaviours.map.setHighlights([]);

    set_map_and_index_source('/resource/', 'floating');
    set_detail_source(pagejs_ctx.path);

    Hijax.behaviours.map.setScope('world');
    if(app_history.length == 1) {
      Hijax.behaviours.map.scheduleViewChange('highlights');
    }

    next();
  }

  function route_static(pagejs_ctx) {
    log.debug('APP route_static', pagejs_ctx);

    if(pagejs_ctx.path != initialization_source.pathname) {
      window.location = pagejs_ctx.path;
    }
  }

  function route_login(pagejs_ctx) {
    log.debug('APP route_login', pagejs_ctx);

    $.get('/.login', function(){
      window.location = "/";
    });
  }

  function route_default(pagejs_ctx, next) {
    log.debug('APP route_default', pagejs_ctx);
    if(! pagejs_ctx.native_fragment) {
      get(pagejs_ctx.path, function(data, textStatus, jqXHR){
        log.debug('APP getting main from:', pagejs_ctx.path)
        get_main(data, pagejs_ctx.path);
      });
    }
    next();
  }

  function routing_done(pagejs_ctx) {
    log.debug('APP routing_done', pagejs_ctx);

    $.when(
      map_and_index_loaded,
      detail_loaded
    ).done(function(){
      Hijax.layout('triggered by routing_done');
      $('#app').removeClass('loading');
    });
  }

  function to_modal(content, opened_on) {
    var modal = $('#app-modal');
    modal.find('.modal-body').append( content );
    modal.data('is_protected', false);
    modal.data('opened_on', opened_on);
    modal.data('url', window.location);
    if(opened_on == 'load') {
      modal.data('url_before', app_history[app_history.length - 2].canonicalPath);
    }
    modal.modal('show');
  }

  var my = {

    init : function(context) {

      if(!init_app) {
        Hijax.attachBehaviours(context, 'without app');
        page('*', function(pagejs_ctx){
          if(pagejs_ctx.path != initialization_source.pathname) {
            window.location = pagejs_ctx.path;
          }
        });
        page();

        my.initialized.resolve();
        return;
      }

      // render template / cleanup dom

      $('body', context).append(
        templates.app({
          header : $('header', context)[0].outerHTML,
          footer : $('footer', context)[0].outerHTML,
          user : user,
          permissions : permissions
        })
      );

      // header & footer
      Hijax.attachBehaviours($('body', context).find('#page-header').add('#page-footer'), 'for header and footer');

      $('body>header, body>main, body>footer', context).remove();

      $('footer', context).removeClass(); // to remove default (non app) styling

      // setup app routes

      page('/', route_start, route_index, routing_done);
      page('/resource/', route_start, route_index, routing_done);
      page('/resource/:id', route_start, route_detail, routing_done);
      page('/country/:id', route_start, route_index_country, routing_done);

      // setup non-app (currently static) and login routes

      page(new RegExp('(' + static_pages.join('|').replace(/\//g, '\\\/') + ')'), route_start, route_static, routing_done);
      page('/.login', route_start, route_login, routing_done);

      // after all app routes ...

      page('*', route_start, route_default, routing_done);

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
        if(col.is('#app-col-index') && $('#app-col-detail').attr('data-col-mode') == 'hidden') {
          page('/');
        } else if(col.is('#app-col-index') && $('#app-col-detail').attr('data-col-mode') == 'expanded') {
          page(detail_source);
        } else if(col.is('#app-col-detail') && $('#app-col-index').attr('data-col-mode') == 'floating') {
          page('/');
        } else if(col.is('#app-col-detail') && $('#app-col-index').attr('data-col-mode') == 'list') {
          page(window.location.pathname + window.location.search);
        }
        Hijax.layout('triggered by column toggle');
      });

      // catch links to fragments

      $('#app', context).on('click', '[data-app="link-to-fragment"] a', function(e){
        my.linkToFragment( this.href.split('/').pop() );
        e.preventDefault();
        e.stopPropagation();
      });

      // bind modal links

      $('#app', context).on('click', '[data-app~="to-modal-and-attach-behaviours"]', function(e){
        e.preventDefault();

        var id = this.hash.substring(1);
        var content = $( this.hash ).clone().prop('id', id + '-clone');
        var is_protected = ( $(this).data('app').indexOf("modal-protected") >= 0 ? true : false );
        var modal = $('#app-modal');

        modal.find('.modal-body').append( content );
        modal.data('is_protected', is_protected);
        modal.data('opened_on', 'click');
        modal.data('url', window.location.pathname + window.location.search + this.hash);
        Hijax.attachBehaviours($('#app-modal'), 'triggered by to-modal-and-attach-behaviours');
        modal.modal('show');
      });

      // catch closing of protected modals and go back if opened on load

      $('#app-modal').on('hide.bs.modal', function(e){
        var modal = $('#app-modal');
        if( modal.data('is_protected') ) {
          var confirm = window.confirm("Are you sure you want to close? All form content will be lost.");
          if(!confirm) {
            return false;
          }
        }
        if(
          modal.data('opened_on') == 'load' &&
          ! modal.data('hidden_on_exit')
        ) {
          if(modal.data('url_before')) {
            log.debug('APP paging to url_before', modal.data('url_before'));
            page(modal.data('url_before'));
          } else {
            log.debug('APP no url_before, paging to home');
            page('/');
          }
        } else if( modal.data('hidden_on_exit') ) {
          modal.data('hidden_on_exit', false);
        }
      });

      // clear modal content when closed and unset protection

      $('#app-modal').on('hidden.bs.modal', function(){
        $('#app-modal').find('.modal-body').empty();
        $('#app-modal').data('is_protected', false)
      });

      // prevent form submit when enter is pressed

      $('#app-modal').on("keypress", "input", function(e) {
          if (e.keyCode == 13) {
            e.preventDefault();
          }
      });

      // catch form submition inside app

      $('#app').on('submit', 'form', function() {
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

      // catch form submition inside modals and handle it async

      $('#app-modal').on('submit', 'form', function(e){
        e.preventDefault();

        var form = $(this);

        $.ajax({
          type : 'POST',
          url : form.attr('action'),
          data : form.serialize(),
          success : function(data, textStatus, jqXHR) {

            var content_type = jqXHR.getResponseHeader('Content-Type');

            // in any case, get contents and attach behaviours
            if(content_type.indexOf("text/plain") > -1) {
              var contents = data;
            } else {
              var contents = get_main(data, form.attr('action'));
            }

            // get the location header, because if a resource was successfully created the response is forwarding to it
            var location = jqXHR.getResponseHeader('Location');

            if(location) {

              // null map and index source as it's outdated
              map_and_index_source = '';

              // close modal
              $('#app-modal').data('is_protected', false).modal('hide');

              var just_a_parser = document.createElement('a');
              just_a_parser.href = location;
              page(just_a_parser.pathname);

            } else if(form.attr('action') == detail_source) {

              // if updated resource is currently lodaded in the detail column, update column and close modal
              $('#app-col-detail [data-app="col-content"]').html( contents );
              $('#app-modal').data('is_protected', false).modal('hide');
              Hijax.layout('triggered by modal submit because it updated the detail_source');

            } else {

              $('#app-modal').find('.modal-body')
                .empty()
                .append( contents );

            }

          },
          error : function(jqXHR, textStatus, errorThrown){

            var content_type = jqXHR.getResponseHeader('Content-Type');

            // in any case, get contents and attach behaviours
            if(content_type.indexOf("text/plain") > -1) {
              var contents = jqXHR.responseText;
            } else {
              var contents = get_main(jqXHR.responseText, form.attr('action'));
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

      /* --- notifications --- */

      $('#app', context).on('click', '[data-app~="close-notification"]', function(e){
        var notification = $(this).closest('.notification');

        if(
          notification.data('dismissable') &&
          notification.find('[name="dismiss"]')[0].checked
        ) {
          var dismissed = JSON.parse( localStorage.getItem('dismissed-notifications') ) || [];
          dismissed.push(notification.data('id'));
          localStorage.setItem('dismissed-notifications', JSON.stringify(dismissed));
        }

        notification.fadeOut(function(){
          $(this).remove();
        });
      });

      $('#app', context).on('click', function(e){
        if(
          ! $(e.target).closest('.notification').length ||
          $(e.target).is('a')
        ) {
          $('.notification:not(#app-notification-prototype)').fadeOut(function(){
            $(this).remove();
          });
        }
      });

      // deferr
      log.debug('APP initialized');
      my.initialized.resolve();
    },

    attach : function(context) {

      if(!init_app) {
        return;
      }

      /* --- modals --- */

      $(context).find('[data-app~="to-modal-on-load"]').each(function(){
        var content = $( this ).children().clone();
        var modal = $('#app-modal');
        var is_protected = ( $(this).data('app').indexOf("modal-protected") >= 0 ? true : false );
        modal.find('.modal-body').empty().append( content );
        modal.data('is_protected', is_protected);
        modal.data('opened_on', 'load');
        modal.data('url', window.location.pathname + window.location.search);
        if(app_history.length > 1) {
          log.debug('APP saving url_before', app_history[app_history.length - 2].canonicalPath);
          modal.data('url_before', app_history[app_history.length - 2].canonicalPath);
        } else {
          log.debug('APP saving / as url_before');
          modal.data('url_before', '/');
        }
        Hijax.attachBehaviours($('#app-modal'), 'triggered by to-modal-on-load');
        modal.modal('show');
      });

      /* --- notifications --- */

      $(context).find('[data-app~="notification"]').each(function(){
        var dismissed = JSON.parse( localStorage.getItem('dismissed-notifications') ) || [];
        if(dismissed.indexOf( this.id ) > -1) {
          return;
        }

        var content = $( this ).children().clone();
        var dismissable = ( $(this).data('app').indexOf("notification-dismissable") >= 0 ? true : false );
        var notification = $('#app-notification-prototype')
          .clone()
          .prop('id', 'notification-' + this.id)
          .data('id', this.id)
          .data('dismissable', dismissable);

        if(dismissable) {
          notification.addClass('dismissable');
        }

        notification
          .find('.notification-content')
          .append( content );

        $('#app-notification-area').append(notification);
      });

    },

    initialized : new $.Deferred(),
    attached : [],

    linkToFragment : function(fragment) {
      if (window.location.pathname.split('/')[1] == 'country') {
        page(window.location.pathname + '#' + fragment);
      } else if( window.location.search ) {
        page('/resource/' + window.location.search + '#' + fragment);
      } else {
        page('/resource/' + fragment);
      }
    }

  };

  Hijax.behaviours.app = my;

  return Hijax;

})(jQuery, Hijax, page);
