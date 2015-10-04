Handlebars.registerHelper('stripProtocol', function (context) {
  
  // strip protocoll
  context = context.replace(/.*?:\/\//g, "");
  
  // also strip trailing slash
  context = context.replace(/\/$/, "");
  
  return context;
})