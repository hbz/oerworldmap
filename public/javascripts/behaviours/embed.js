// --- embed ---

var Hijax = (function ($, Hijax) {

  var my = {
    attach: function(context) {
      $('a', context).attr("target", "_blank");
    },
    attached : []
  }

  Hijax.behaviours.embed = my;

  Hijax.goto = function(url) {
    window.open(url);
  }

  Hijax.behaviours.app.linkToFragment = function(id) {
    window.open("/resource/" + id, '_blank').focus();
  }

  return Hijax;

})(jQuery, Hijax);
