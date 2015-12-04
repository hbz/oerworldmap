if (typeof String.prototype.endsWith !== 'function') {
  String.prototype.endsWith = function(suffix) {
    return this.indexOf(suffix, this.length - suffix.length) !== -1;
  };
}


Handlebars.registerHelper('stripProtocol', function (context) {

  // strip protocoll
  context = context.replace(/.*?:\/\//g, "");

  // also strip trailing slash
  context = context.replace(/\/$/, "");

  return context;

});
