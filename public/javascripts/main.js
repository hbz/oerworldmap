// accent folding for bloodhound
// https://github.com/twitter/typeahead.js/issues/271#issuecomment-121720571

var bloodhoundAccentFolding = {

  charMap : {
    'a': /[àáâã]/gi,
    'c': /[ç]/gi,
    'e': /[èéêë]/gi,
    'i': /[ïí]/gi,
    'o': /[ôó]/gi,
    'oe': /[œ]/gi,
    'u': /[üú]/gi
  },

  normalize : function (str) {
    $.each(bloodhoundAccentFolding.charMap, function (normalized, regex) {
      str = str.replace(regex, normalized);
    });
    return str;
  },

  queryTokenizer : function (q) {
    var normalized = bloodhoundAccentFolding.normalize(q);
    return Bloodhound.tokenizers.whitespace(normalized);
  }

};


(function ($, document) {

  // --- set js class on <html>

  document.documentElement.className = 'js';

  // --- document ready ---

  $(document).ready(function() {

    var page = window.location.pathname.substring(1);

    if(
      page != "contribute" &&
      page != "FAQ" &&
      page != "about" &&
      page != "contribute" &&
      page != "imprint"
    ) {
      $('body').addClass("layout-fixed");
    }

    Hijax.initBehaviours(document);

  });

})(jQuery, document);
