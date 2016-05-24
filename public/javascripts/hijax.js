var Hijax = (function (window) {

  var deferreds = {};

  var my = {

    behaviours : {},

    attachBehaviour : function(context, behaviour) {
      if ('function' == typeof(my.behaviours[behaviour].attach)) {
        console.log("Attaching", behaviour, context);
        if ('function' == typeof(my.behaviours[behaviour].init)) {
          my.behaviours[behaviour].initialized.done(function() {
            my.behaviours[behaviour].attach(context);
          });
        } else {
          my.behaviours[behaviour].attach(context);
        }
      }
    },

    attachBehaviours : function(context) {
      for (behaviour in my.behaviours) {
        my.attachBehaviour(context, behaviour);
      }
      return context;
    },

    initBehaviour : function(context, behaviour) {
      if ('function' == typeof(my.behaviours[behaviour].init)) {
        console.log("Initializing", behaviour, context);
        my.behaviours[behaviour].init(context);
        my.attachBehaviour(context, behaviour);
      } else {
        my.attachBehaviour(context, behaviour);
      }
    },

    initBehaviours : function(context) {
      for (var behaviour in my.behaviours) {
        my.initBehaviour(context, behaviour);
      }
      return context;
    },

    layout : function() {
      for (var behaviour in my.behaviours) {
        if(my.behaviours[behaviour].layout) {
          console.log("Laying out", behaviour);
          my.behaviours[behaviour].layout();
        }
      }
    },

    goto : function(url) {
      window.location = url;
    },

  }

  return my;

})(window);
