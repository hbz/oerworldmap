Hijax.behaviours.mapResource = {

  attach : function(context) {
    var that = this;

    $('article.resource-story', context)
      .add($('div.resource-organization', context))
      .each(function() {
        that.populate( this );
        Hijax.behaviours.map.setBoundingBox();
      });

  },

  populate : function(article) {

    var json = JSON.parse( $(article).find('script').html() );

    var markers = Hijax.behaviours.map.getMarkers(json, Hijax.behaviours.map.getResourceLabel);

    Hijax.behaviours.map.addPlacemarks( markers );

  }

};
