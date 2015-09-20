Handlebars.registerHelper('getTitle', function (string, options) {
  var parts = string.split('.');
  return parts[parts.length -1];
});