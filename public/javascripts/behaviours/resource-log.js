var Hijax = (function ($, Hijax) {

  function showDiff(fromURL, toURL, context) {
    $.get(toURL, function(toData) {
      $.get(fromURL, function(fromData) {
        var diff = $(htmldiff($(fromData).filter('main').find('article').html(), $(toData).filter('main').find('article').html()));
        diff.find('del').css({'background-color': 'rgba(255, 0, 0, 0.5)', 'text-decoration': 'none'});
        diff.find('ins').css({'background-color': 'rgba(0, 255, 0, 0.5)', 'text-decoration': 'none'});
        $('#diff', context).html(diff);
        return;
      });
    });
  }

  var my = {

    attach: function(context) {

      $('[data-behaviour="resource-log"]').each(function() {
        $('input', context).on('change', function() {
          var from = $('input[name=from]:checked', context).val() || 'HEAD';
          var to = $('input[name=to]:checked', context).val()  || 'HEAD';
          window.location.hash = from + '..' + to;
        });

        window.onhashchange = function() {
          var url = $('#resource', context).attr('href');
          var range = window.location.hash.substr(1).split("..");
          var fromURL = url + '?version=' + range[0];
          var toURL = url + '?version=' + range[1];
          showDiff(fromURL, toURL, context);
        }

        $("input:radio[name=from]:eq(1)", context).click();
        $("input:radio[name=to]:eq(0)", context).click();
      });

    }

  }

  Hijax.behaviours['resource-log'] = my;
  return Hijax;

})(jQuery, Hijax);
