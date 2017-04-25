// --- embed ---

var Hijax = (function ($, Hijax) {

  Hijax.behaviours.app.linkToFragment = function(id) {
    window.open("/resource/" + id, '_blank').focus();
  }

  return Hijax;

})(jQuery, Hijax);
