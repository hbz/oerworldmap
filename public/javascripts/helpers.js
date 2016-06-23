function trim_at_first_blank(s) {
  if(s.indexOf(' ') > -1) {
    return s.substring(0, s.indexOf(' ')) + ' ...';
  } else {
    return s;
  }
}

Handlebars.registerHelper('stripProtocol', function (context) {

  // strip protocoll
  context = context.replace(/.*?:\/\//g, "");

  // also strip trailing slash
  context = context.replace(/\/$/, "");

  return context;

});
