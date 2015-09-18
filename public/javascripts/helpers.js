Handlebars.registerHelper('removeFilterLink', function (filter, href) {
  var matchFilter = new RegExp("[?&]filter=" + filter, "g");
  var matchFrom = new RegExp("from=\\d+", "g");
  return new Handlebars.SafeString(
    href.replace(matchFilter, '').replace(matchFrom, 'from=0')
  );
});

Handlebars.registerHelper('addFilterLink', function (filter, value, href) {
  var matchFrom = new RegExp("from=\\d+", "g");
  return new Handlebars.SafeString(
    href.replace(matchFrom, 'from=0') + "&filter=" + filter + ':' + value
  );
});

Handlebars.registerHelper('notIn', function (item, list, options) {
  var found = false;
  for (i in list) {
    if (list[i] == item) {
      found = true;
    }
  }
  if (!found) {
    return options.fn(this);
  } else {
    return options.inverse(this);
  }
});