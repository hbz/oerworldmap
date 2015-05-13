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

    var markers = Hijax.behaviours.map.getMarkers(json, function(resource) {
      switch (resource['@type']) {
        case 'Person':
          return resource['name'][0]['@value'];
        case 'Organization':
          return resource['legalName']['@value'];
        default:
          return resource['@id'];
      }
    });

    Hijax.behaviours.map.addPlacemarks( markers );

  }

};
