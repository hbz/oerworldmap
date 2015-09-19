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

Handlebars.registerHelper('ifIn', function (item, list, options) {
  for (i in list) {
    if (list[i] == item) {
      return options.fn(this);
    }
  }
  return options.inverse(this);
});