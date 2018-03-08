// --- embed ---

var Hijax = (function ($, Hijax) {

  if (window.embed == 'true') {
    Hijax.behaviours.app.linkToFragment = function(id) {
      window.open("/resource/" + id, '_blank').focus();
    }
  }

  return Hijax;

})(jQuery, Hijax);
