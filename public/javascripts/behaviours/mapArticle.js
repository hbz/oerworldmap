Hijax.behaviours.mapArticle = {

  attach : function(context) {
    var that = this;

    $('article.resource-story', context).each(function() {
      that.populate( this );
      Hijax.behaviours.map.setBoundingBox();
    });

  },

  populate : function(article) {

    var json = JSON.parse( $(article).find('script').html() );

    var markers = Hijax.behaviours.map.getMarkers(json, function(resource) {
      return resource['name'][0]['@value'];
    });

    Hijax.behaviours.map.addPlacemarks( markers );

  }

};
