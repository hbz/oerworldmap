var Hijax = (function ($, Hijax) {

  var toHtml;

  var my = {

    attach: function(context) {

      $.get($('#resource', context).attr('href'), function(data) {
        toHtml = $(data).filter('main').find('article').html();
        $('#to', context).html(toHtml);
      });

      $('[data-behaviour="resource-log"] pre a', context).bind('click', function() {
        $.get(this.getAttribute('href'), function(data) {
          var fromHtml = $(data).filter('main').find('article').html();
          var from = $(htmldiff(fromHtml, toHtml));
          from.find('ins').remove();
          from.find('del').css({'background-color': 'rgba(255, 0, 0, 0.5)', 'text-decoration': 'none'});
          var to = $(htmldiff(fromHtml, toHtml));
          to.find('del').remove();
          to.find('ins').css({'background-color': 'rgba(0, 255, 0, 0.5)', 'text-decoration': 'none'});
          $('#to', context).html(to);
          $('#from', context).html(from);
        });
        return false;
      });


    }

  }

  Hijax.behaviours['resource-log'] = my;
  return Hijax;

})(jQuery, Hijax);
