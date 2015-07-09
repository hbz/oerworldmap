Hijax.behaviours.populateMap = {

  attach : function(context) {
    var that = this;

    // don't use class names for js actions -> reorganize behaviours
    $('.populate-map', context).each(function(){
      that.populatePlacemarks( this );
    });

    $('[about="#users-by-country"]', context).each(function(){
      that.populateHeatdata( this );
      $(this).find('tr').hide();
    });
  },

  populatePlacemarks : function(list, context) {
    var json = JSON.parse( $(list).find('script[type="application/ld+json"]').html() );
    var markers = [];

    for (i in json) {
      var markers = markers.concat(Hijax.behaviours.map.getMarkers(json[i], Hijax.behaviours.map.getResourceLabel));
    }

    Hijax.behaviours.map.addPlacemarks( markers );

  },

  populateHeatdata : function(list) {
    var json = JSON.parse( $(list).find('script[type="application/ld+json"]').html() );
    Hijax.behaviours.map.setAggregations( json );
  }

};
