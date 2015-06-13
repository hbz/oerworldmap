Hijax.behaviours.map = {

  container : null,
  world : null,
  vector : null,
  projection: null,

  attach : function(context) {
    var map = this;

    $('div[data-view="map"]', context).each(function() {

      proj4.defs('ESRI:53009', '+proj=moll +lon_0=0 +x_0=0 +y_0=0 +a=6371000 ' +
          '+b=6371000 +units=m +no_defs');

      // Configure the Sphere Mollweide projection object with an extent,
      // and a world extent. These are required for the Graticule.
      var sphereMollweideProjection = new ol.proj.Projection({
        code: 'ESRI:53009',
        extent: [-9009954.605703328, -9009954.605703328,
          9009954.605703328, 9009954.605703328],
        worldExtent: [-179, -90, 179, 90]
      });

      //map.projection = sphereMollweideProjection;
      map.projection = ol.proj.get('EPSG:3857');
      //map.projection = ol.proj.get('WGS84');

      // Map container
      map.container = $('<div id="map"></div>')[0];
      $(this).prepend(map.container);

      // Vector layer
      map.vector = new ol.layer.Vector({
        source: new ol.source.Vector({
          url: '/assets/json/ne_50m_admin_0_countries_topo.json',
          format: new ol.format.TopoJSON(),
          noWrap: true,
          wrapX: false
        })
      });

      // Map object
      map.world = new ol.Map({
        layers: [map.vector],
        target: map.container,
        view: new ol.View({
          center: [0, 0],
          projection: map.projection,
          zoom: 1
        })
      });

      var graticule = new ol.Graticule({
        map: map.world,
        strokeStyle: new ol.style.Stroke({
          color: 'rgba(255,120,0,0.9)',
          width: 1.5,
          lineDash: [0.5, 4]
        })
      });

      map.world.on('pointermove', function(evt) {
        if (evt.dragging) {
          return;
        }
        var pixel = map.world.getEventPixel(evt.originalEvent);
        var hit = map.world.hasFeatureAtPixel(pixel);
        map.world.getTarget().style.cursor = hit ? 'pointer' : '';
        map.displayFeatureInfo(pixel, context);
      });

      map.world.on('click', function(evt) {
        var feature = map.world.forEachFeatureAtPixel(evt.pixel, function(feature, layer) {
          return feature;
        });
        if (feature) {
          var properties = feature.getProperties();
          if (properties.url) {
            window.location = properties.url;
          } else {
            window.location = "/country/" + feature.getId().toLowerCase();
          }
        }
      });

      var that = this;
      // FIXME: Naive workaround for asychronous loading, should be ol.source.Vector.loader
      setTimeout(function(){ map.setBoundingBox(that); }, 1000);

    });

  },

  displayFeatureInfo : function(pixel, context) {
    var map = this;

    var feature = map.world.forEachFeatureAtPixel(pixel, function(feature, layer) {
      return feature;
    });

    var info = $('[about="#users-by-country"]', context);
    info.find('tr').hide();
    info.find('thead>tr').hide();
    if (feature) {
      var properties = feature.getProperties();
      if (properties.type) {
        // Feature is an icon to which the resource and references are attached
        console.log(properties.resource);
        console.log(properties.refBy);
      } else if (properties.country) {
        // Feature is a country
        console.log(properties.country);
        info.find('thead>tr').show();
        info.find('tr[about="#' + feature.getId().toLowerCase() + '"]').show();
      }
    }

  },

  setHeatmapData : function(aggregations) {

    var map = this;

    var heat_data = {};

    for(var j = 0; j < aggregations["entries"].length; j++) {
      var aggregation = aggregations["entries"][j];
      heat_data[ aggregation.key.toUpperCase() ] = 0;
      for(var i = 0; i < aggregation["observations"].length; i ++) {
        heat_data[ aggregation.key.toUpperCase() ] += aggregation["observations"][i].value;
      }
    }

    var heats = $.map(heat_data, function(value, index) {
      return [value];
    });

    var color = d3.scale.log()
      .range(["#a1cd3f", "#eaf0e2"])
      .interpolate(d3.interpolateHcl)
      .domain([d3.quantile(heats, .01), d3.quantile(heats, .99)]);

    map.vector.setStyle(function(feature) {
      return [new ol.style.Style({
        fill: new ol.style.Fill({
          color: heat_data[ feature.getId() ] ? color( heat_data[ feature.getId() ] ) : "#ffffff"
        }),
        stroke: new ol.style.Stroke({
          color: '#0c75bf',
          width: .5
        })
      })];
    });

  },

  addPlacemarks : function(placemarks) {

    var map = this;

    var vectorSource = new ol.source.Vector({
      features: placemarks,
      noWrap: true,
      wrapX: false
    });

    /*var clusterSource = new ol.source.Cluster({
      distance: 40,
      source: vectorSource,
      noWrap: true,
      wrapX: false
    });

    var styleCache = {};
    var clusterLayer = new ol.layer.Vector({
      source: clusterSource,
      style: function(feature, resolution) {
        var size = feature.get('features').length;
        var style = styleCache[size];
        if (!style) {
          style = [new ol.style.Style({
            image: new ol.style.Circle({
              radius: 10,
              stroke: new ol.style.Stroke({
                color: '#fff'
              }),
              fill: new ol.style.Fill({
                color: '#3399CC'
              })
            }),
            text: new ol.style.Text({
              text: size.toString(),
              fill: new ol.style.Fill({
                color: '#fff'
              })
            })
          })];
          styleCache[size] = style;
        }
        return style;
      }
    });
    map.world.addLayer(clusterLayer);*/

    var vectorLayer = new ol.layer.Vector({
      source: vectorSource
    });

    map.world.addLayer(vectorLayer);

  },

  setBoundingBox : function(element) {

    var map = this;

    var focusId = element.getAttribute("data-focus");
    if (focusId) {
      map.world.getLayers().forEach(function(layer) {
        var feature = layer.getSource().getFeatureById(focusId);
        if (feature) {
          var extent = feature.getGeometry().getExtent();
          if (extent[0] == extent[2]) {
            extent[0] -= 1000000;
            extent[2] += 1000000;
          }
          if (extent[1] == extent[3]) {
            extent[1] -= 1000000;
            extent[3] += 1000000;
          }
          map.world.getView().fitExtent(extent, map.world.getSize());
        }
      });
    }

  },

  markers : {},
  getMarkers : function(resource, labelCallback, origin) {

    origin = origin || resource;
    var that = this;

    if (that.markers[resource['@id']]) {
      for (var i = 0; i < that.markers[resource['@id']].length; i++) {
        var properties = that.markers[resource['@id']][i].getProperties();
        if (!properties.refBy[origin['@id']] && origin['@id'] != resource['@id']) {
          properties.refBy[origin['@id']] = origin;
        }
        that.markers[resource['@id']][i].setProperties(properties);
      }
      return that.markers[resource['@id']];
    }

    var locations = [];
    var markers = [];

    if (resource.location && resource.location instanceof Array) {
      locations = locations.concat(resource.location);
    } else if (resource.location) {
      locations.push(resource.location);
    }

    for (var l in locations) {
      if (geo = locations[l].geo) {
        var point = new ol.geom.Point(ol.proj.transform([geo['lon'], geo['lat']], 'EPSG:4326', that.projection.getCode()));

        var color;
        switch (resource['@type']) {
          case 'Article':
            color = 'red';
            break;
          case 'Organization':
            color = 'blue';
            break;
          default:
            color = 'black';
        }

        var iconStyle = new ol.style.Style({
          text: new ol.style.Text({
            text: '\uf041',
            font: 'normal 1.5em FontAwesome',
            textBaseline: 'Bottom',
            fill: new ol.style.Fill({color: color})
          })
        });

        var featureProperties = {
          resource: resource,
          refBy: {},
          geometry: point,
          url: "/resource/" + resource['@id'],
          type: resource['@type'],
        };
        featureProperties.refBy[origin['@id']] = origin;

        var feature = new ol.Feature(featureProperties);
        feature.setId(resource['@id']);
        feature.setStyle(iconStyle);
        markers.push(feature);
      }
    }


    if (!markers.length) {
      for (var key in resource) {
        var value = resource[key];
        if (value instanceof Array) {
          for (var i = 0; i < value.length; i++) {
            if (typeof value[i] == 'object') {
              markers = markers.concat(
                that.getMarkers(value[i], labelCallback, origin)
              );
            }
          }
        } else if (typeof value == 'object') {
          markers = markers.concat(
            that.getMarkers(value, labelCallback, origin)
          );
        }
      }
    }

    that.markers[resource['@id']] = markers;
    return markers;

  }

}
