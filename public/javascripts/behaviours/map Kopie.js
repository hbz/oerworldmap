// --- map ---
Hijax.behaviours.map = {

  attach: function(context) {

    var map = $('#worldmap', context);
    var width = $(document).width() * 0.5;
    var height = width / 1.5;
    map.css("width", width);
    map.css("height", height);

    // init vector map
    map.vectorMap({
      backgroundColor: '#0c75bf',
      zoomButtons: false,
      zoomOnScroll: false,
      series: {
        regions: [{
          values: {},
          scale: ['#cfdfba', '#a1cd3f'],
          normalizeFunction: 'linear'
        }]
      },
      onRegionTipShow: function(e, el, code) {
        var country_champion = false;
        var users_registered = false;
        var initiatives_registered = false;

        if (
          $('ul[about="#country-champions"] li[data-country-code="' + code + '"]', context).length
        ) {
          country_champion = true;
        }

        if (
          false
        ) {
          users_registered = true;
        }

        if (
          false
        ) {
          initiatives_registered = true;
        }

        el.html(
          (
            users_registered ?
            '<i class="fa fa-fw fa-user"></i> <strong>' + heat_data[code] + '</strong> users counted for ' + el.html() + (
              $('section#user-register form', context).length ?
              ' (Click to be the next ...)<br>' :
              ''
            ) :
            '<i class="fa fa-fw fa-user"></i> No users counted for ' + el.html() + (
              $('section#user-register form', context).length ?
              ' (Click to be the first ...)<br>' :
              ''
            )
          ) + (
            initiatives_registered ?
            '<i class="fa fa-fw fa-users"></i> <strong>' + story_data[code] + '</strong> initiatives counted for ' + el.html() + '<br>' :
            ''
          ) + (
            country_champion ?
            '<i class="fa fa-fw fa-trophy"></i> And we have a country champion!<br>' :
            ''
          )
        );
      },
      onRegionClick: function(e, code) {
        if (!$('section#user-register form', context).length) return false;
        $('select[name="workLocation[address][addressCountry]"]', context).val(code);
        $('html, body', context).animate({
          scrollTop: $('#user-register', context).offset().top - 100
        }, 500, function() {
          if (history.pushState) {
            history.pushState(null, null, '#user-register');
          } else {
            // window.location.hash = link_hash_divided[1];
          }
        });
      },
      markerStyle: {
        initial: {
          fill: '#f09711',
          stroke: '#0c75bf',
          r: 8
        }
      }
    });

  }

};

Hijax.behaviours.heatmap = {

  attach: function(context) {
    var mapObject = $('#worldmap').vectorMap('get', 'mapObject');
    var heat_table = $('table[about="#users-by-country"]', context);
    var heat_json = heat_table.length ? JSON.parse(heat_table.find('script').html()) : {};
    var heat_data = {};

    // hide table
    heat_table.hide();

    // convert heat map data
    for (i in heat_json.entries) {
      heat_data[heat_json.entries[i].key.toUpperCase()] = heat_json.entries[i].value;
    }

    // example heat map data
    if (true) {
      heat_data = {
        "DE": 15,
        "CH": 4,
        "AT": 6,
        "GB": 12,
        "FR": 9,
        "ES": 5,
        "US": 9,
        "PL": 2,
        "BF": 1,
        "NO": 5,
        "CN": 6,
        "ID": 4,
        "GH": 4,
        "IR": 5,
        "BR": 7,
        "CD": 5,
        "KZ": 9,
        "RU": 2,
        "RO": 4,
        "DZ": 3,
        "CA": 2
      };
    }

    function data_max(arr) {
      return Math.max.apply(null, Object.keys(arr).map(function(e) {return arr[e];}));
     }
    function data_min(arr) {
      return Math.min.apply(null, Object.keys(arr).map(function(e) {return arr[e];}));
    }

    mapObject.series.regions[0].clear();
    mapObject.series.regions[0].scale.setMin(data_min(heat_data));
    mapObject.series.regions[0].scale.setMax(data_max(heat_data));
    mapObject.series.regions[0].setValues(heat_data);

  }
};

Hijax.behaviours.map_stories = {

  attach: function(context) {
    // process story locations
    var mapObject = $('#worldmap').vectorMap('get', 'mapObject');
    var story_list = $('ul.resource-list', context);
    var story_json = story_list.length ? JSON.parse(story_list.find('script').html()) : {};
    var story_data = {};
    var story_markers = [];

    for (i in story_json) {
      var locations = [];
      for (j in story_json[i].location) {
        locations.push(story_json[i].location[j]);
      }
      for (k in story_json[i].agent) {
        if (story_json[i].agent[k].location) {
          locations.push(story_json[i].agent[k].location);
        }
      }
      for (l in locations) {
        if (country = locations[l].address.addressCountry) {
          if (story_data[country]) {
            story_data[country]++;
          } else {
            story_data[country] = 1;
          }
        }
        if (geo = locations[l].geo) {
          story_markers.push({
            latLng: [geo['lon'], geo['lat']],
            name: story_json[i]['name'][0]['@value']
          })
        }
      }
    }

    mapObject.addMarkers(story_markers, []);

  }

};
