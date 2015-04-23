Hijax = {

  initialize: function() {
    Hijax.attachBehaviours(document)
  },

  attachBehaviours: function(context) {
    for (behaviour in Hijax.behaviours) {
      Hijax.behaviours[behaviour].attach(context);
    }
    return context;
  },

  behaviours: {},

}
