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

    layout : function() {
/*

      for (var behaviour_to_layout in my.behaviours) {

        console.log('behaviour_to_layout', behaviour_to_layout);

        if('function' == typeof my.behaviours[behaviour_to_layout].init) {

          console.log('behaviour_to_layout init', behaviour_to_layout);
          var foo = behaviour_to_layout;
          my.behaviours[behaviour_to_layout].initialized.done(function() {
            console.log('behaviour_to_layout deffered', foo);
            if('function' == typeof my.behaviours[foo].layout) {
              my.behaviours[foo].layout();
            }
          });
        } else if('function' == typeof my.behaviours[behaviour_to_layout].layout) {
          my.behaviours[behaviour_to_layout].layout();
        }
      }
*/

      for (var behaviour in my.behaviours) {

        if('function' == typeof my.behaviours[behaviour].layout) {

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
