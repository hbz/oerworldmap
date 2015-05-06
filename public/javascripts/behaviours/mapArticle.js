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

    var data = {};
    var markers = [];

    var locations = [];
    for (j in json.location) {
      locations.push(json.location[j]);
    }
    for (k in json.mentions) {
      if (json.mentions[k].location) {
        locations.push(json.mentions[k].location);
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
          name: json['name'][0]['@value'],
          url: "/resource/" + json['@id']
        })
      }
    }

    Hijax.behaviours.map.addPlacemarks( markers );

  }

};
