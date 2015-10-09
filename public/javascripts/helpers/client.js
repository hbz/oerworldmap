Handlebars.registerHelper('i18n', function (key, options) {
  var bundle = options.hash.bundle || 'messages';
  return new Handlebars.SafeString(i18nStrings[bundle][key] || key);
});
