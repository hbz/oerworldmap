var Hijax = (function ($, Hijax) {

  var container = null,
      world = null,
      view = null,
      projection = null,
      popover,
      popoverElement,
      countryVectorSource = null,
      countryVectorLayer = null,
      subNationalVectorSource = null,
      subNationalVectorLayer = null,
      placemarksVectorSource = null,
      placemarksVectorLayer = null,
      clusterSource = null,
      clusterLayer = null,
      mapboxTileSource = null,
      mapboxTileLayer = null,
      hoveredCountriesOverlay = null,

      standartMapSize = [896, 655],
      standartInitialZoom = 1.85,
      standartMinZoom = 1.85,
      standartMaxZoom = 15,

      defaultCenter = [0, 5000000],
      currentCenter = false,

      iconCodes = {
        'organizations': 'users',
        'users': 'user',
        'stories': 'comment'
      },

      colors = {
        'blue-darker': '#2d567b',
        'orange': '#fe8a00'
      },

      markers = {},

      current_pins = [],
      current_highlights = [],

      templates = {},

      styles = {
        placemark : {

          base : function() {
            return new ol.style.Style({
              text: new ol.style.Text({
                text: '\uf041',
                font: 'normal 1.5em FontAwesome',
                textBaseline: 'Bottom',
                fill: new ol.style.Fill({
                  color: colors['blue-darker']
                }),
                stroke: new ol.style.Stroke({
                  color: 'white',
                  width: 4
                })
              })
            });
          },

          hover : function() {
            return new ol.style.Style({
              text: new ol.style.Text({
                text: '\uf041',
                font: 'normal 2em FontAwesome',
                textBaseline: 'Bottom',
                fill: new ol.style.Fill({
                  color: colors['orange']
                }),
                stroke: new ol.style.Stroke({
                  color: 'white',
                  width: 4
                })
              })
            });
          }

        },
        cluster : {

          base : function(count) {
            return new ol.style.Style({
              image: new ol.style.Circle({
                radius: 13,
                stroke: new ol.style.Stroke({
                  color: '#fff',
                  width: 2
                }),
                fill: new ol.style.Fill({
                  color: '#244267'
                })
              }),
              text: new ol.style.Text({
                text: count.toString(),
                textAlign: 'center',
                font: 'normal 13px "Source Sans Pro"',
                fill: new ol.style.Fill({
                  color: '#fff'
                })
              })
            });
          },

          hover : function(count) {
            return new ol.style.Style({
              image: new ol.style.Circle({
                radius: 13,
                stroke: new ol.style.Stroke({
                  color: '#fff',
                  width: 2
                }),
                fill: new ol.style.Fill({
                  color: '#fe8a00'
                })
              }),
              text: new ol.style.Text({
                text: count.toString(),
                textAlign: 'center',
                font: 'normal 13px "Source Sans Pro"',
                fill: new ol.style.Fill({
                  color: '#fff'
                })
              })
            });
          }

        },
        placemark_cluster : {

          base : function(feature) {
            var count = feature.get('features').length;
            if(count > 1) {
              return styles.cluster.base(count);
            } else {
              return styles.placemark.base();
            }
          },

          hover : function(feature) {
            var count = feature.get('features').length;
            if(count > 1) {
              return styles.cluster.hover(count);
            } else {
              return styles.placemark.hover();
            }
          }
        }
      },

      hoverState = {
        id : false
      };

  function getZoomValues() {
    if(
      $('#map').width() / $('#map').height()
      >
      standartMapSize[0] / standartMapSize[1]
    ) {
      // current aspect ratio is more landscape then standart, so take height as constraint
      var size_factor = $('#map').height() / standartMapSize[1];
    } else {
      // it's more portrait, so take width
      var size_factor = $('#map').width() / standartMapSize[0];
    }

    size_factor = Math.pow(size_factor, 0.5);

    return {
      initialZoom: Math.ceil(standartInitialZoom * size_factor),
      minZoom: Math.round(standartMinZoom * size_factor),
      maxZoom: Math.round(standartMaxZoom * size_factor)
    };
  }

  function getFeatureType(feature) {
    if(feature.getId()) {
      return 'country';
    } else if(feature.get('features').length > 1) {
      return "cluster";
    } else {
      return "placemark";
    }
  }

  function getFeatureTypeById(id) {
    if(id.length == 2) {
      return "country";
    } else {
      return "placemark";
    }
  }

  function updateHoverState(pixel) {

    // get feature at pixel
    var feature = world.forEachFeatureAtPixel(pixel, function(feature, layer) {
      return feature;
    });

    // get feature type and so on
    if(feature) {

      var type = getFeatureType( feature );

      if(type == 'country' || type == 'placemark' || type == 'cluster') {
        var show_popover = true;
      } else {

      }

      if(feature.getId()) {
        var id = feature.getId();
      } else {
        var id = feature.get('features')[0].getId();
      }
    }

    // the statemachine ...
    if(
      ! hoverState.id &&
      feature
    ) {
      // ... nothing hovered yet, but now

      if(show_popover) {
        $(popoverElement).show();
        setPopoverContent( feature, type );
        setPopoverPosition( feature, type, pixel );
      }
      setFeatureStyle( feature, 'hover' );
      hoverState.id = id;
      hoverState.feature = feature;
      world.getTarget().style.cursor = 'pointer';

    } else if(
      hoverState.id &&
      feature &&
      id == hoverState.id
    ) {
      // ... same feature was hovered

      if(show_popover) {
        setPopoverPosition( feature, type, pixel );
      }

    } else if(
      hoverState.id &&
      feature &&
      id != hoverState.id
    ) {
      // ... another feature was hovered

      if(show_popover) {
        $(popoverElement).show();
        setPopoverContent( feature, type )
        setPopoverPosition( feature, type, pixel );
      } else {
        $(popoverElement).hide();
      }
      setFeatureStyle( feature, 'hover' );
      resetFeatureStyle( hoverState.feature );

      hoverState.id = id;
      hoverState.feature = feature;

    } else if(
      hoverState.id &&
      ! feature
    ) {
      // ... a feature was hovered but now noone is

      $(popoverElement).hide();
      resetFeatureStyle( hoverState.feature );
      hoverState.id = false;
      hoverState.feature = false;
      world.getTarget().style.cursor = '';

    } else {

      // ... do nothing probably – or did i miss somehting?

    }

    // FIXME @j0hj0h: this should probably go somewhere else
    if (world.getView().getZoom() > 8 && (type == "country")) {
      $(popoverElement).hide();
      hoveredCountriesOverlay.setVisible(false);
      world.getTarget().style.cursor = '';
    } else if (show_popover) {
      $(popoverElement).show();
      hoveredCountriesOverlay.setVisible(true);
      world.getTarget().style.cursor = 'pointer';
    } else {
      hoveredCountriesOverlay.setVisible(true);
    }

  }

  function setFeatureStyle( feature, style ) {

    var feature_type = getFeatureType( feature );

    if( feature_type == 'country' ) {
      hoveredCountriesOverlay.getSource().addFeature(feature);
      return;
    }

    feature.setStyle(
      styles[ "placemark_cluster" ][ style ](feature)
    );
  }

  function resetFeatureStyle( feature ) {
    var feature_type = getFeatureType( feature );

    if( feature_type == 'placemark' || feature_type == 'cluster' ) {
      var properties = feature.getProperties();
      if(! properties.highligted) {
        setFeatureStyle(
          feature,
          'base'
        );
      }
    }

    if( feature_type == 'country' ) {
      hoveredCountriesOverlay.getSource().removeFeature(
        feature
      );
    }

  }

  function setPopoverPosition(feature, type, pixel) {

    var coord = world.getCoordinateFromPixel(pixel);

    if( type == 'country' ) {
      popover.setPosition(coord);
      popover.setOffset([0, -20]);
    }

    if( type == 'placemark' || type == 'cluster' ) {
      // calculate offset first
      // ... see http://gis.stackexchange.com/questions/151305/get-correct-coordinates-of-feature-when-projection-is-wrapped-around-dateline
      // ... old offest code until commit ae99f9886bdef1c3c517cd8ea91c28ad23126551
      var offset = Math.floor((coord[0] / 40075016.68) + 0.5);
      var popup_coord = feature.getGeometry().getCoordinates();
      popup_coord[0] += (offset * 20037508.34 * 2);

      popover.setPosition(popup_coord);
      popover.setOffset([0, -30]);
    }
  }

  function setPopoverContent(feature, type) {
/*
    if( type == "placemark" ) {
      var properties = feature.get('features')[0].getProperties();
    } else {
      var properties = feature.getProperties();
    }
*/

    if( type == "placemark" ) {
      var properties = feature.get('features')[0].getProperties();
      var content = templates['popover' + properties.type](properties);
    }

    if( type == "cluster" ) {
      var content = '';
      var features = feature.get('features');

      if(features.length < 4) {
        for(var i = 0; i < features.length; i++) {
          var properties = features[i].getProperties();
          content += templates['popover' + properties.type](properties);
        }
      } else {
        var aggregation = {};
        for(var i = 0; i < features.length; i++) {
          var properties = features[i].getProperties();
          aggregation[ properties.type ] = aggregation[ properties.type ] + 1 || 1;
        }
        content += templates['popoverCluster'](aggregation);
      }

    }

    if( type == "country") {
      var properties = feature.getProperties();

      // setup empty countrydata, if undefined
      if( typeof properties.country == 'undefined' ) {
        properties.country = {
          key : feature.getId()
        }
      }

      var country = properties.country;
      country.key = country.key.toUpperCase();
      var content = templates.popoverCountry(country);
    }

    $(popoverElement).find('.popover-content').html( content );

  }

  function setCountryData(aggregations) {

    if (!countryVectorSource) return;

    // attach aggregations to country features

    for(var j = 0; j < aggregations["about.location.address.addressCountry"]["buckets"].length; j++) {
      var aggregation = aggregations["about.location.address.addressCountry"]["buckets"][j];
      var feature = countryVectorSource.getFeatureById(aggregation.key.toUpperCase());
      if (feature) {
        var properties = feature.getProperties();
        properties.country = aggregation;
        feature.setProperties(properties);
      } else {
        throw 'No feature with id "' + aggregation.key.toUpperCase() + '" found';
      }
    }

  }

  function setHeatmapColors(aggregations) {

    var heat_data = {};

    // determine focused country

    var focusId = $('[data-behaviour="map"]').attr("data-focus");

    if(
      focusId &&
      getFeatureTypeById(focusId) == "country"
    ) {
      var focused_country = focusId;
    } else {
      var focused_country = false;
    }

    // build heat data hashmap

    for(var j = 0; j < aggregations["about.location.address.addressCountry"]["buckets"].length; j++) {
      var aggregation = aggregations["about.location.address.addressCountry"]["buckets"][j];
      heat_data[ aggregation.key.toUpperCase() ] = aggregation["doc_count"];
    }

    // setup d3 color callback

    var heats_arr = _.values(heat_data);
    var get_color = d3.scale.log()
      .range(["#a1cd3f", "#eaf0e2"])
      .interpolate(d3.interpolateHcl)
      .domain([d3.quantile(heats_arr, .01), d3.quantile(heats_arr, .99)]);

    // set style callback

    countryVectorLayer.setStyle(function(feature) {

      if(
        ! focused_country
      ) {
        var stroke_width = world.getView().getZoom() > 4 ? 1.5 : 0.5;
        var stroke_color = '#0c75bf';
        var zIndex = 1;

        var color = heat_data[ feature.getId() ] ? get_color( heat_data[ feature.getId() ] ) : "#fff";
      } else if(
        focused_country != feature.getId()
      ) {
        var stroke_width = world.getView().getZoom() > 4 ? 1.5 : 0.5;
        var stroke_color = '#0c75bf';
        var zIndex = 1;

        var color_rgb = heat_data[ feature.getId() ] ? get_color( heat_data[ feature.getId() ] ) : "#fff";
        var color_d3 = d3.rgb(color_rgb);
        var color = "rgba(" + color_d3.r + "," + color_d3.g + "," + color_d3.b + ",0.9)";
      } else {
        var stroke_width = world.getView().getZoom() > 4 ? 2 : 1.5;
        var stroke_color = '#0c75bf';
        var zIndex = 2;

        var color = heat_data[ feature.getId() ] ? get_color( heat_data[ feature.getId() ] ) : "#fff";
      }

      return [new ol.style.Style({
        fill: new ol.style.Fill({
          color: color
        }),
        stroke: new ol.style.Stroke({
          color: stroke_color,
          width: stroke_width
        }),
        zIndex: zIndex
      })];

    });

  }

  function addPlacemarks(placemarks) {
    current_pins = placemarks;
  }

  function highlightPlacemarks(placemarks) {
    current_highlights = placemarks;
  }

  function setBoundingBox(element, layouted) {

    var dataFocus = element.getAttribute("data-focus");
    var boundingBox;

    // determine boundingBox or focusIds from focus

    if (dataFocus == "fit") {

      boundingBox = placemarksVectorSource.getExtent();

    } else if(dataFocus == "fit-highlighted") {

      var focusIds = [];
      $.each(current_highlights, function(i, v){
        focusIds.push(v.getId());
      });

    } else if(dataFocus == "re-center") {

      var focusIds = false;
      if(! currentCenter) {
        dataFocus == "fit";
        boundingBox = placemarksVectorSource.getExtent();
      }

    } else if(dataFocus == "") {

      var focusIds = false;

    } else {

      // should be list of ids in this case
      var focusIds = dataFocus.trim().split(" ");

    }

    // if focusIds are set, determine bounding box for them

    if (focusIds) {

      // Init bounding box and transformation function
      var boundingBox = ol.extent.createEmpty();
      var tfn = ol.proj.getTransform('EPSG:4326', projection.getCode());

      // Look for features on all layers and extend bounding box
      world.getLayers().forEach(function(layer) {

        for (var i = 0; i < focusIds.length; i++) {

          if(layer.get('title') == 'cluster') {

            var clusters = layer.getSource().getFeatures();
            for(var j = 0; j < clusters.length; j++) {
              var features = clusters[ j ].get('features');
               for(var k = 0; k < features.length; k++) {
                if (features[ k ].getId() == focusIds[i]) {
                  ol.extent.extend(boundingBox, features[ k ].getGeometry().getExtent());
                }
               }
            }

          } else if (layer.getSource().getFeatureById) {

            var feature = layer.getSource().getFeatureById(focusIds[i]);
            if (feature) {
              ol.extent.extend(boundingBox, feature.getGeometry().getExtent());
            }

          }
        }
      });

    }

    // if at this point there is a bounding box, that's not infinit, fit the view to it

    if (boundingBox && (boundingBox[0] != Infinity)) {

      log.debug('MAP setBoundingBox – fitting to bounding box');
      world.getView().fit(boundingBox, world.getSize(), {
        minResolution: 2
      });
      currentCenter = world.getView().getCenter();
      layouted.resolve();

    } else {

      // if no bounding box, look at dataFocus again

      if(dataFocus == 're-center') {

        log.debug('MAP setBoundingBox – only updating the center');
        world.getView().setCenter(currentCenter);

      } else if(dataFocus == '') {

        // set center to user and zoom to initial again ...

        if ( navigator.geolocation ) {
          var got_user_location = new $.Deferred();
          navigator.geolocation.getCurrentPosition(function(position) {

            log.debug('MAP setBoundingBox – set center to user position');
            var lon = position.coords.longitude;
            var center = ol.proj.transform([lon, 0], 'EPSG:4326', projection.getCode());
            center[1] = defaultCenter[1];
            world.getView().setCenter(center);
            got_user_location.resolve();

          }, function(err){

            log.debug('MAP setBoundingBox – set center to default');
            world.getView().setCenter(defaultCenter);
            got_user_location.resolve();

          });
        } else {

          log.debug('MAP setBoundingBox – set center to default');
          world.getView().setCenter(defaultCenter);

        }

        // Get zoom values adapted to map size
        log.debug('MAP setBoundingBox – set zoom to initial');
        var zoom_values = getZoomValues();
        world.getView().setZoom(zoom_values.initialZoom);

        currentCenter = defaultCenter;
      }

      if(typeof got_user_location !== 'undefined') {
        got_user_location.done(function(){
          // restrictListToExtent();
          layouted.resolve();
        })
      } else {
        // restrictListToExtent();
        layouted.resolve();
      }
    }
  }

  function zoomToFeatures(features) {
    var extent = ol.extent.createEmpty();
    for(var i = 0; i < features.length; i++) {
      ol.extent.extend(extent, features[i].getGeometry().getExtent());
    }
    world.getView().fit(extent, world.getSize(), {
      minResolution: 2
    });
  }

  function getMarkers(resource, labelCallback, origin) {
    origin = origin || resource;

    if (markers[resource['@id']]) {
      for (var i = 0; i < markers[resource['@id']].length; i++) {
        var properties = markers[resource['@id']][i].getProperties();
        if (properties.resource['@id'] != origin['@id'] && properties.resource.referencedBy) {
          var referenced = properties.resource.referencedBy.reduce(function(previousValue, currentValue, index, array) {
            return (previousValue || ( currentValue['@id'] == origin['@id'] ));
          }, false);
          if (!referenced) {
            properties.resource.referencedBy.push(origin);
          }
        } else if (resource['@id'] != origin['@id']) {
          properties.resource.referencedBy = [origin];
        }
        markers[resource['@id']][i].setProperties(properties);
      }
      return markers[resource['@id']];
    }

    var locations = [];
    var _markers = [];

    if (resource.location && resource.location instanceof Array) {
      locations = locations.concat(resource.location);
    } else if (resource.location) {
      locations.push(resource.location);
    }

    for (var l in locations) {
      if (geo = locations[l].geo) {
        var point = new ol.geom.Point(ol.proj.transform([geo['lon'], geo['lat']], 'EPSG:4326', projection.getCode()));
        if (resource['@id'] != origin['@id'] && resource.referencedBy) {
          resource.referencedBy.push(origin);
        } else if (resource['@id'] != origin['@id']) {
          resource.referencedBy = [origin];
        }
        var featureProperties = {
          resource: resource,
          geometry: point,
          url: "/resource/" + resource['@id'],
          type: resource['@type'],
        };

        var feature = new ol.Feature(featureProperties);
        feature.setId(resource['@id']);
        feature.setStyle(styles['placemark']['base']()); // ... seems not to be necessary
        _markers.push(feature);
      }
    }

    var traverse = [ "mainEntity", "mentions", "member", "agent", "participant", "provider" ]

    if (!_markers.length) {
      for (var key in resource) {
        if (traverse.indexOf(key) == -1) {
          continue;
        }
        var value = resource[key];
        if (value instanceof Array) {
          for (var i = 0; i < value.length; i++) {
            if (typeof value[i] == 'object') {
              _markers = _markers.concat(
                getMarkers(value[i], labelCallback, origin)
              );
            }
          }
        } else if (typeof value == 'object') {
          _markers = _markers.concat(
            getMarkers(value, labelCallback, origin)
          );
        }
      }
    }

    if (_markers.length && resource['@id']) {
      markers[resource['@id']] = _markers;
    }

    return _markers;

  }

  function set_style_for_clusters_containing_markers(markers, style, highligted){
    highligted = highligted || false;

    // iterate over markers and collect the clusters, they are in
    var clusters = [];
    for(var i = 0; i < markers.length; i++) {
      clusterLayer.getSource().forEachFeature(function(f){
        var cfeatures = f.get('features');
        for(var j = 0; j < cfeatures.length; j++) {
          if(markers[i].getId() == cfeatures[j].getId()) {
            clusters.push(f);
          }
        }
      });
    }

    // last but not least, change the style of the clusters, we found
    for(var i = 0; i < clusters.length; i++) {
      clusters[i].setStyle( styles.placemark_cluster[ style ]( clusters[i] ) );
      if(highligted) {
        var properties = clusters[i].getProperties();
        properties.highligted = true;
        clusters[i].setProperties(properties);
      }
    }
  }

  function restrictListToExtent(e) {

    var container = $('[data-behaviour~="geoFilteredList"]');

    if (
      container.length &&
      (
        typeof e == 'undefined' ||
        (
          (e.key == 'resolution' || e.key == 'center') &&
          $('#app-col-index').attr('data-col-mode') == 'list'
        )
      )
    ) {

      log.debug('MAP restrictListToExtent');

      var list = container.find('.resource-list');
      var enabled = container.find('.geo-filtered-list-control input');

      if (enabled.prop("checked")) {
        var extent = world.getView().calculateExtent(world.getSize());
        list.children('li').hide();
        placemarksVectorSource.forEachFeatureInExtent(extent, function(feature) {
          var resource = feature.getProperties()['resource'];
          var ids = resource['referencedBy'] ? resource['referencedBy'].map(function(obj){return obj['@id']}) : [];
          ids.push(resource['@id']);
          for (var i = 0; i < ids.length; i++) {
            list.children('li[about="' + ids[i] + '"]').show();
          }
        });
      } else {
        list.children('li').show();
      }

      container.find('.total-items').text(list.children('li:visible').length);
    }
  }

  var my = {
    init : function(context) {

      if (!$('div[data-behaviour="map"]', context).length) {
        my.initialized.resolve();
        return;
      }

      // Get mercator projection
      projection = ol.proj.get('EPSG:3857');

      // Override extents
      (
        function() {
          var overrides = {
            "FR": [[-8, 52], [15, 41]],
            "RU": [[32, 73], [175, 42]],
            "US": [[-133, 52], [-65, 25]],
            "NL": [[1, 54], [10, 50]],
            "NZ": [[160, -32], [171, -50]]
          };
          var getGeometry = ol.Feature.prototype.getGeometry;
          var transform = ol.proj.getTransform('EPSG:4326', projection.getCode());
          ol.Feature.prototype.getGeometry = function() {
            var result = getGeometry.call(this);
            var id = this.getId();
            if (id in overrides) {
              result.getExtent = function() {
                return ol.extent.applyTransform(ol.extent.boundingExtent(overrides[id]), transform);
              }
            }
            return result;
          }
        }
      )();

      $('div[data-behaviour="map"]', context).each(function() {

        // switch style
        // $(this).addClass("map-view");
        // $('body').removeClass("layout-scroll").addClass("layout-fixed");

        // Map container
        container = $('<div id="map"></div>')[0];
        $(this).prepend(container);

        // Country vector source
        countryVectorSource = new ol.source.Vector({
          url: '/assets/json/ne_50m_admin_0_countries_topo.json',
          format: new ol.format.TopoJSON(),
          // noWrap: true,
          wrapX: true
        });

        // Country vector layer
        countryVectorLayer = new ol.layer.Vector({
          title: 'country',
          source: countryVectorSource
        });

        // Subnational vector source
        /*
        subNationalVectorSource = new ol.source.Vector({
          url: '/assets/json/ne_10m_admin_1_states_provinces_topo.json',
          format: new ol.format.TopoJSON(),
          // noWrap: true,
          wrapX: true
        });

        // Subnational vector layer
        subNationalVectorLayer = new ol.layer.Vector({
          title: 'sub',
          source: subNationalVectorSource
        });
        */

        // Mapbox tile source
        var mapboxKey = "pk.eyJ1IjoibGl0ZXJhcnltYWNoaW5lIiwiYSI6ImNpZ3M1Y3pnajAyNGZ0N2tuenBjN2NkN2oifQ.TvQji1BZcWAQBfYBZcULwQ";
        mapboxTileSource = new ol.source.XYZ({
          attributions: '© <a href="https://www.mapbox.com/map-feedback/">Mapbox</a> ' +
            '© <a href="http://www.openstreetmap.org/copyright">' +
            'OpenStreetMap contributors</a>',
          tileSize: [512, 512],
          url: 'https://api.mapbox.com/styles/v1/literarymachine/ciq3njijr004kq7nduyya7hxg/tiles/{z}/{x}/{y}?access_token=' + mapboxKey
        });

        // Mapbox tile layer
        mapboxTileLayer = new ol.layer.Tile({
          source: mapboxTileSource
        });

        // Placemark Layer
        placemarksVectorSource = new ol.source.Vector({
          wrapX: true
        });
        placemarksVectorLayer = new ol.layer.Vector({
          source: placemarksVectorSource
        });

        // Cluster layer
        clusterSource = new ol.source.Cluster({
          distance: 40,
          source: placemarksVectorSource,
          noWrap: true,
          wrapX: false
        });
        clusterLayer = new ol.layer.Vector({
          title: 'cluster',
          source: clusterSource,
          style: function(feature, resolution) {
            return [styles.placemark_cluster.base(feature)];
          }
        });

        // Get zoom values adapted to map size
        var zoom_values = getZoomValues();

        // View
        view = new ol.View({
          center: defaultCenter,
          projection: projection,
          zoom: zoom_values.initialZoom,
          minZoom: zoom_values.minZoom,
          maxZoom: zoom_values.maxZoom
        });

        // overlay for country hover style
        var collection = new ol.Collection();
        hoveredCountriesOverlay = new ol.layer.Vector({
          title: 'country-hover',
          source: new ol.source.Vector({
            features: collection,
            useSpatialIndex: false // optional, might improve performance
          }),
          style: function(feature, resolution) {
            return [new ol.style.Style({
              stroke: new ol.style.Stroke({
                color: '#0c75bf',
                width: world.getView().getZoom() > 4 ? 2 : 1.5
              })
            })];
          },
          updateWhileAnimating: false, // optional, for instant visual feedback
          updateWhileInteracting: false // optional, for instant visual feedback
        });

        // Map object
        world = new ol.Map({
          //layers: [subNationalVectorLayer, countryVectorLayer, mapboxTileLayer, hoveredCountriesOverlay, clusterLayer],
          layers: [countryVectorLayer, mapboxTileLayer, hoveredCountriesOverlay, clusterLayer],
          target: container,
          view: view,
          controls: ol.control.defaults({ attribution: false })
        });

        // User position
/*
        if (
          navigator.geolocation &&
          ! $(this).attr('data-focus')
        ) {
          navigator.geolocation.getCurrentPosition(function(position) {
            var lon = position.coords.longitude;
            var center = ol.proj.transform([lon, 0], 'EPSG:4326', projection.getCode());
            center[1] = defaultCenter[1];
            world.getView().setCenter(center);
          });
        }
*/

        // Bind hover events
        world.on('pointermove', function(evt) {
          if (evt.dragging) { return; }
          var pixel = world.getEventPixel(evt.originalEvent);
          updateHoverState(pixel);
        });

        world.on('moveend', function(evt) {
          set_style_for_clusters_containing_markers(current_highlights, 'hover', true);
        });

        // Bind click events
        world.on('click', function(evt) {
          var feature = world.forEachFeatureAtPixel(evt.pixel, function(feature, layer) {
            return feature;
          });
          if (feature) {
            var type = getFeatureType(feature)
            if (type == "placemark") {
              Hijax.behaviours.app.linkToFragment( feature.get("features")[0].getProperties()['resource']['@id'] );
            } else if(type == "country" && world.getView().getZoom() < 9) {
              page("/country/" + feature.getId().toLowerCase());
            } else if(type == "cluster") {
              zoomToFeatures(feature.get("features"));
              if ($('#app-col-index').attr('data-col-mode') == 'floating') {
                $('#app-col-map [data-behaviour="map"]').attr('data-focus', 're-center');
                page('/resource/?q=*');
              }
            }
          }
        });

        // restrict list to extent
        world.getView().on('propertychange', _.debounce(restrictListToExtent, 500));

        // update currentCenter
        world.getView().on('propertychange', _.debounce(function(e) {
          if (e.key == 'center' || e.key == 'resolution') {
            currentCenter = world.getView().getCenter();
          }
        }, 150));

        // init popover
        popoverElement = $('<div class="popover fade top in layout-typo-small" role="tooltip"><div class="arrow"></div><div class="popover-content"></div></div>')[0];
        popover = new ol.Overlay({
          element: popoverElement,
          positioning: 'bottom-center',
          stopEvent: false,
          wrapX: true
        });
        world.addOverlay(popover);
        $(container).mouseleave(function(){
          if(hoverState.feature) {
            $(popoverElement).hide();
            resetFeatureStyle( hoverState.feature );
            hoverState.id = false;
            hoverState.feature = false;
          }
        });

        // precompile handlebar templates
        templates = {
          popoverAction : Handlebars.compile($('#popoverResource\\.mustache').html()),
          popoverOrganization : Handlebars.compile($('#popoverResource\\.mustache').html()),
          popoverPerson : Handlebars.compile($('#popoverResource\\.mustache').html()),
          popoverService : Handlebars.compile($('#popoverResource\\.mustache').html()),
          popoverEvent : Handlebars.compile($('#popoverResource\\.mustache').html()),

          popoverCountry : Handlebars.compile($('#popoverCountry\\.mustache').html()),
          popoverCluster : Handlebars.compile($('#popoverCluster\\.mustache').html())
        };

      });

      // Defer until vector source is loaded
      if (countryVectorSource.getFeatureById("US")) { // Is this a relieable test?
        log.debug('MAP initialized');
        my.initialized.resolve();
      } else {
        var listener = countryVectorSource.on('change', function(e) {
          if (countryVectorSource.getState() == 'ready') {
            ol.Observable.unByKey(listener);
            log.debug('MAP initialized');
            my.initialized.resolve();
          }
        });
      }

    },

    layouted : false,

    layout : function() {

      $.when.apply(null, my.attached).done(function(){

        log.debug('MAP layout started (waited to be attached therefor)');

        // Ensure that all current highlights are in current pins, too
        if (current_pins.length) {
          for (var i = 0; i < current_highlights.length; i++) {
            for (var j = 0; j < current_pins.length; j++) {
              if (current_highlights[i].getId() == current_pins[j].getId()) {
                current_pins.splice(j, 1);
              }
            }
            current_pins.push(current_highlights[i]);
          }
        } else {
          current_pins = current_highlights;
        }

        clusterSource.clear();
        placemarksVectorSource.clear();
        placemarksVectorSource.addFeatures(current_pins);

        set_style_for_clusters_containing_markers(current_highlights, 'hover', true);

        // check if the behaviours layouted (created at the beginning of attach is resolved already
        // if so create a local one, otherwise pass the behaviours one ...
        if(layouted.isResolved()) {
          var layouted = new $.Deferred();
          setBoundingBox($('[data-behaviour="map"]')[0], layouted);
        } else {
          setBoundingBox($('[data-behaviour="map"]')[0], layouted);
        }

        layouted.done(function(){
          log.debug('MAP layout finished');
        });

      });

      world.updateSize();
    },

    clearHighlights : function() {
      current_highlights = [];
    },

    attach : function(context, attached) {

      function get_markers_from_json(json) {
        var _markers = [];
        if(json instanceof Array) {
          for (i in json) {
            _markers = _markers.concat( getMarkers(json[i]) );
          }
        } else {
          _markers = getMarkers(json, Hijax.behaviours.map.getResourceLabel);
        }
        return _markers;
      }

      // creating layouted deferred at the beginning of attached, to schedule actions from inside attach
      // to happen after subsequent layout

      layouted = $.Deferred();

      // Populate map with pins from resource listings

      markers = {};
      $('[data-behaviour~="populateMap"]', context).each(function(){
        var json = JSON.parse( $(this).find('script[type="application/ld+json"]').html() );
        addPlacemarks(
          get_markers_from_json(json)
        );
      });

      // Hide entries not in current map extent by examining all markers (including "indirect" ones)
      // for each list entry
      $('[data-behaviour~="geoFilteredList"]', context).each(function(){

        var container = $(this);

        // var checked = $('#app-col-index').attr('data-col-mode') == 'list' ? 'checked="checked"' : '';
        var checked = 'checked="checked"';
        var enabled = $('<input type="checkbox" name="enabled" ' + checked + ' />').change(function() {
          restrictListToExtent();
        });

        layouted.done(function(){
          restrictListToExtent();
        });

        container.find('.geo-filtered-list-control').prepend($('<label> Search as I move the map</label>').prepend(enabled));
      });

      // Populate pin highlights

      $('[data-behaviour~="populateMapHightlights"]', context).each(function(){
        var json = JSON.parse( $(this).find('script[type="application/ld+json"]').html() );
        highlightPlacemarks(
          get_markers_from_json(json)
        );
      });


      // Link list entries to pins
      // ... quite lengthy. Could need some refactoring. Probably by capsulating the resource/pin connection.
      $('[data-behaviour~="linkedListEntries"]', context).each(function(){

        $( this ).on("mouseenter", "li", function() {

          var id = this.getAttribute("about");
          var script = $(this).closest("ul").children('script[type="application/ld+json"]');

          if (script.length) {

            // first get the markers that represent hovered resource
            var json = JSON.parse( script.html() );
            var resource = json.filter(function(resource) {
              return resource['@id'] == id;
            })[0];
            var markers = getMarkers(resource);

            set_style_for_clusters_containing_markers(markers, 'hover');
          }
        });

        $( this ).on("mouseleave", "li", function() {

          var id = this.getAttribute("about");
          var script = $(this).closest("ul").children('script[type="application/ld+json"]');

          if (script.length) {

            // first get the markers that represent hovered resource
            var json = JSON.parse( script.html() );
            var resource = json.filter(function(resource) {
              return resource['@id'] == id;
            })[0];
            var markers = getMarkers(resource);

            set_style_for_clusters_containing_markers(markers, 'base');

/*
            // iterate over markers and collect the clusters, they are in
            var clusters = [];
            for(var i = 0; i < markers.length; i++) {
              clusterLayer.getSource().forEachFeature(function(f){
                var cfeatures = f.get('features');
                for(var j = 0; j < cfeatures.length; j++) {
                  if(markers[i].getId() == cfeatures[j].getId()) {
                    clusters.push(f);
                  }
                }
              });
            }

            // last but not least, change the style of the clusters, we found
            for(var i = 0; i < clusters.length; i++) {
              clusters[i].setStyle(styles.placemark_cluster.base(clusters[i]));
            }
*/

          }

        });
      });

      // Add heat map data
      $('form#form-resource-filter', context).add($('#country-statistics', context)).each(function(){
        var json = JSON.parse( $(this).find('script[type="application/ld+json"]#json-aggregations').html() );
        setHeatmapColors( json );
        if( $(this).is('table') ) {
          $(this).hide();
        }
      });

      $('#global-statistics', context).each(function(){
        var json = JSON.parse( $(this).find('script[type="application/ld+json"]').html() );
        setCountryData( json );
      });

      // Set zoom
/*
      $('[data-behaviour="map"]', context).each(function() {
        // zoom to bounding box, if focus is set
        setBoundingBox(this);
      });
*/

      attached.resolve();

    },

    initialized : new $.Deferred(),

    attached : []

  };

  Hijax.behaviours.map = my;

  return Hijax;

})(jQuery, Hijax);
