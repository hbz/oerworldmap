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
