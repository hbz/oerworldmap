Hijax.behaviours.populateMap = {
  
  attach : function(context) {
    var that = this;
    
    $('.resource-list', context).each(function(){
      that.populate( this );
    });
    
  },
  
  populate : function(list) {
    var json = JSON.parse( $(list).find('script').html() );
    var markers = [];

    for (i in json) {
      var markers = markers.concat(Hijax.behaviours.map.getMarkers(json[i], function(resource) {
        return resource['name'][0]['@value'];
      }));
    }

    Hijax.behaviours.map.addPlacemarks( markers );
  }
  
};
