function scroll_to_element(scroll_container, element) {
  $( scroll_container ).animate({
    scrollTop: $( element ).position().top
  }, 500);
}

Handlebars.registerHelper('i18n', function (key, options) {
  var bundle = options.hash.bundle || 'messages';
  return new Handlebars.SafeString(i18nStrings[bundle][key] || key);
});

Handlebars.registerHelper('stringFormat', function() {

  var args = Array.prototype.slice.call(arguments);
  var format = args[0];
  var params = args.slice(1, args.length - 1);
  var options = args[args.length - 1];

  var size = format.match(/%./g).length;
  for (var i = 0; i < size; i++) {
    format = format.replace(/%./, params[i]);
  }

  return new Handlebars.SafeString(format);

});

Handlebars.registerHelper('jsonscript', function(obj) {

  return new Handlebars.SafeString(
    '<script type="application/ld+json">' +
      JSON.stringify(obj, null, 2) +
    '</script>'
  );

});

window.queryString = function(url) {
  a = url ? url.search.substr(1).split('&') : window.location.search.substr(1).split('&');
  if (a == "") return {};
  var b = {};
  for (var i = 0; i < a.length; ++i) {
      var p = a[i].split('=', 2);
      var param = decodeURIComponent(p[0]);
      if (p.length == 1) {
        b[param] = "";
      } else if (b[p[0]] instanceof Array) {
        b[param].push(decodeURIComponent(p[1].replace(/\+/g, " ")));
      } else if (b[p[0]]) {
        b[param] = [b[p[0]], decodeURIComponent(p[1].replace(/\+/g, " "))];
      } else {
        b[param] = decodeURIComponent(p[1].replace(/\+/g, " "));
      }
  }
  return b;
};
