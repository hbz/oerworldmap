Handlebars.registerHelper('stripProtocol', function (context) {
  return context.replace(/.*?:\/\//g, "");
})