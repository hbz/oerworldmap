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
    },
    
    getResourceId : function() {
      var path = window.location.pathname;
      console.log(path.split('/'));
      
      if(
        path.split('/').length == 3 &&
        path.split('/')[1] == "resource" &&
        path.split('/')[2]
      ) {
        return path.split('/')[2];
      } else {
        return false;
      }
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
