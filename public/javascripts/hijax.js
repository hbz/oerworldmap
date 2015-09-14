var Hijax = (function (window) {

  var deferreds = {};

  var my = {

    behaviours : {},

    attachBehaviour : function(context, behaviour) {
      if ('function' == typeof(my.behaviours[behaviour].attach)) {
        if (behaviour in deferreds) {
          deferreds[behaviour].done(function() {
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
        deferreds[behaviour] = my.behaviours[behaviour].init(context);
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

    goto : function(url) {
      window.location = url;
    },

  }

  return my;

})(window);
