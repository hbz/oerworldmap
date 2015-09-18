Handlebars.registerHelper('removeFilterLink', function (filter, href) {
  var matchFilter = new RegExp("[?&]filter=" + filter, "g");
  var matchFrom = new RegExp("from=\\d+", "g");
  return new Handlebars.SafeString(
    href.replace(matchFilter, '').replace(matchFrom, 'from=0')
  );
});