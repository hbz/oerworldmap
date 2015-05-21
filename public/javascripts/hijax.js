Hijax = {

  functions : {
    // https://css-tricks.com/snippets/javascript/get-url-variables/
    getQueryVariable : function(variable) {
      var query = window.location.search.substring(1);
      var vars = query.split("&");
      for (var i=0;i<vars.length;i++) {
        var pair = vars[i].split("=");
        if (pair[0] == variable) {
          return decodeURIComponent(pair[1]);
        }
      }
      return(false);
    }
  },

  attachBehaviours: function(context) {
    for (behaviour in Hijax.behaviours) {
      Hijax.behaviours[behaviour].attach(context);
    }
    return context;
  },

  behaviours: {},

}
