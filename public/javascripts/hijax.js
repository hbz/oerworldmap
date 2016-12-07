var Hijax = (function (window) {

  var deferreds = {};

  var my = {

    behaviours : {},

    attachBehaviour : function(context, behaviour, msg) {
      if ('function' == typeof(my.behaviours[behaviour].attach)) {
        var attached = new $.Deferred();
        my.behaviours[behaviour].attached.push(attached);
        if ('function' == typeof(my.behaviours[behaviour].init)) {
          // log.debug('there is an init to wait for ...', behaviour);
          my.behaviours[behaviour].initialized.done(function() {
            log.debug("attaching (" + msg + "):", behaviour, context);
            my.behaviours[behaviour].attach(context, attached);
          });
        } else {
          log.debug("attaching (" + msg + "):", behaviour, context);
          my.behaviours[behaviour].attach(context, attached);
        }
      }
    },

    attachBehaviours : function(context, msg) {
      log.debug('attaching behaviours (' + msg + ')');
      for (var behaviour in my.behaviours) {
        my.attachBehaviour(context, behaviour, msg);
      }
      return context;
    },

    initBehaviour : function(context, behaviour) {
      if ('function' == typeof(my.behaviours[behaviour].init)) {
        log.debug("initializing:", behaviour, context);
        my.behaviours[behaviour].init(context);
        // log.debug("initializing done:", behaviour, context);
        // my.attachBehaviour(context, behaviour);
      } else {
        // my.attachBehaviour(context, behaviour);
      }
    },

    initBehaviours : function(context) {
      for (var behaviour in my.behaviours) {
        my.initBehaviour(context, behaviour);
      }
      return context;
    },

    layout : function(msg) {
      log.debug('layouting (' + msg + ')');
      for (var behaviour in my.behaviours) {
        if('function' == typeof my.behaviours[behaviour].layout) {
          log.debug('layouting (' + msg + ')', behaviour);
          if('function' == typeof my.behaviours[behaviour].init) {
            var _behaviour = behaviour;
            my.behaviours[behaviour].initialized.done(function() {
              my.behaviours[_behaviour].layout();
            });
          } else {
            my.behaviours[behaviour].layout();
          }
        }
      }
    },

    goto : function(url) {
      window.location = url;
    },

  }

  return my;

})(window);
