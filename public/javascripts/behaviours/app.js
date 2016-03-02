var Hijax = (function ($, Hijax, page) {

  var templates = {
    'app' : Handlebars.compile($('#app\\.mustache').html())
  };

  var map_index_source = '';

  Hijax.behaviours.hfactor = {

    init : function(context) {

      $('body', context).append(
        templates.app({
          header : $('header', context)[0].outerHTML,
          footer : $('footer', context)[0].outerHTML
        })
      );

      page('/', function(ctx){
        var url = '/resource/?' + ctx.querystring;
        if(url != map_index_source) {
          $.get(
            url,
            function(data){
              $('#app-col-index', context).html(
                $(data).filter('main').contents()
              );
              map_index_source = url;
            }
          )
        }
      });
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
