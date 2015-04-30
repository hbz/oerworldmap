Hijax.behaviours.populateMap = {
  
  attach : function(context) {
    var that = this;
    
    $('.resource-list', context).each(function(){
      that.populate( this );
    });
    
  },
  
  populate : function(list) {
    var json = JSON.parse( $(list).find('script').html() );
    
    console.log(json);
    
    var data = {};
    var markers = [];

    for (i in json) {
      var locations = [];
      for (j in json[i].location) {
        locations.push(json[i].location[j]);
      }
      for (k in json[i].agent) {
        if (json[i].agent[k].location) {
          locations.push(json[i].agent[k].location);
        }
      }
      for (l in locations) {
        if (country = locations[l].address.addressCountry) {
          if (data[country]) {
            data[country]++;
          } else {
            data[country] = 1;
          }
        }
        if (geo = locations[l].geo) {
          markers.push({
            latLng: [geo['lat'], geo['lon']],
            name: json[i]['name'][0]['@value'],
            url: "/resource/" + json[i]['@id']
          })
        }
      }
    }
    
    Hijax.behaviours.map.addPlacemarks( markers );
  }
  
};