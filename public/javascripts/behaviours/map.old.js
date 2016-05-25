var Hijax = (function ($, Hijax) {

  var container = null,
      world = null,
      view = null,
      projection = null,
      popover,
      popoverElement,
      countryVectorSource = null,
      countryVectorLayer = null,
      placemarksVectorSource = null,
      placemarksVectorLayer = null,
      clusterSource = null,
      clusterLayer = null,
      osmTileSource = null,
      osmTileLayer = null,
      hoveredCountriesOverlay = null,

      standartMapSize = [896, 655],
      standartInitialZoom = 1.85,
      standartMinZoom = 1.85,
      standartMaxZoom = 15,

      defaultCenter = [0, 5000000],

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
      initialZoom: standartInitialZoom * size_factor,
      minZoom: standartMinZoom * size_factor,
      maxZoom: standartMaxZoom * size_factor
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

      if(type == 'country' || type == 'placemark') {
        var show_popover = true;
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
      setFeatureStyle(
        feature,
        'base'
      );
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
    if( type == "placemark" ) {
      var properties = feature.get('features')[0].getProperties();
    } else {
      var properties = feature.getProperties();
    }

    if( type == "placemark" ) {
      var content = templates['popover' + properties.type](properties);
    }

    if( type == "cluster" ) {
      var content = "cluster ...";
    }

    if( type == "country") {

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

  function setAggregations(aggregations) {

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

    setHeatmapColors(aggregations);

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
        var stroke_width = 0.5;
        var stroke_color = '#0c75bf';
        var zIndex = 1;

        var color = heat_data[ feature.getId() ] ? get_color( heat_data[ feature.getId() ] ) : "#fff";
      } else if(
        focused_country != feature.getId()
      ) {
        var stroke_width = 0.5;
        var stroke_color = '#0c75bf';
        var zIndex = 1;

        var color_rgb = heat_data[ feature.getId() ] ? get_color( heat_data[ feature.getId() ] ) : "#fff";
        var color_d3 = d3.rgb(color_rgb);
        var color = "rgba(" + color_d3.r + "," + color_d3.g + "," + color_d3.b + ",0.9)";
      } else {
        var stroke_width = 2;
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
    clusterSource.clear();
    placemarksVectorSource.clear();
    placemarksVectorSource.addFeatures(placemarks);
  }

  function setBoundingBox(element) {

    var dataFocus = element.getAttribute("data-focus");

    var boundingBox;

    if ("fit" == dataFocus) {

      boundingBox = placemarksVectorSource.getExtent();

    } else {

      var focusIds = dataFocus ? dataFocus.trim().split(" ") : false;

      if (focusIds) {

        // Init bounding box and transformation function
        var boundingBox = ol.extent.createEmpty();
        var tfn = ol.proj.getTransform('EPSG:4326', projection.getCode());

        // Look for features on all layers and extend bounding box
        world.getLayers().forEach(function(layer) {
          for (var i = 0; i < focusIds.length; i++) if (layer.getSource().getFeatureById) {
            var feature = layer.getSource().getFeatureById(focusIds[i]);
            if (feature) {
              ol.extent.extend(boundingBox, feature.getGeometry().getExtent());
            }
          }
        });

        // Special case single point
        if (boundingBox[0] == boundingBox[2]) {
          boundingBox[0] -= 1000000;
          boundingBox[2] += 1000000;
        }
        if (boundingBox[1] == boundingBox[3]) {
          boundingBox[1] -= 1000000;
          boundingBox[3] += 1000000;
        }
      }

    }

    if (boundingBox && (boundingBox[0] != Infinity)) {
      // Set extent of map view
      world.getView().fit(boundingBox, world.getSize(), {
        padding: [50, 50, 50, 50]
      });
    }

  }

  function zoomToFeatures(features) {
    var extent = ol.extent.createEmpty();
    for(var i = 0; i < features.length; i++) {
      ol.extent.extend(extent, features[i].getGeometry().getExtent());
    }
    world.getView().fit(extent, world.getSize(), {
      padding: [50, 50, 50, 50]
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
          source: countryVectorSource
        });

        // OSM tile source
        osmTileSource = new ol.source.OSM({
          url: 'https://{a-c}.tiles.mapbox.com/v4/johjoh.oer_worldmap/{z}/{x}/{y}.png?access_token=pk.eyJ1Ijoiam9oam9oIiwiYSI6Imd3bnowY3MifQ.fk6HYuu3q5LzDi3dyip0Bw'
        });

        // OSM tile layer
        osmTileLayer = new ol.layer.Tile({
          source: osmTileSource,
          preload: Infinity,
          opacity: 1
        });
        osmTileLayer.setVisible(false);

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

        // Show OSM layer when zooming in
        view.on('propertychange', function(e) {
          switch (e.key) {
            case 'resolution':
              if (4 < view.getZoom() && view.getZoom() < 8) {
                osmTileLayer.setVisible(true);
              } else {
                osmTileLayer.setVisible(false);
              }
              break;
          }
        });

        // overlay for country hover style
        var collection = new ol.Collection();
        hoveredCountriesOverlay = new ol.layer.Vector({
          source: new ol.source.Vector({
            features: collection,
            useSpatialIndex: false // optional, might improve performance
          }),
          style: function(feature, resolution) {
            return [new ol.style.Style({
              stroke: new ol.style.Stroke({
                color: '#0c75bf',
                width: 2
              })
            })];
          },
          updateWhileAnimating: false, // optional, for instant visual feedback
          updateWhileInteracting: false // optional, for instant visual feedback
        });

        // Map object
        world = new ol.Map({
          layers: [countryVectorLayer, osmTileLayer, hoveredCountriesOverlay, clusterLayer],
          target: container,
          view: view,
          controls: ol.control.defaults({ attribution: false })
        });

        // User position
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

        // Bind hover events
        world.on('pointermove', function(evt) {
          if (evt.dragging) { return; }
          var pixel = world.getEventPixel(evt.originalEvent);
          updateHoverState(pixel);
        });

        // Bind click events
        world.on('click', function(evt) {
          var feature = world.forEachFeatureAtPixel(evt.pixel, function(feature, layer) {
            return feature;
          });
          if (feature) {
            var type = getFeatureType(feature)
            if (type == "placemark") {
              Hijax.goto('#' + feature.get("features")[0].getProperties()['resource']['@id']);
            } else if(type == "country") {
              Hijax.goto("/country/" + feature.getId().toLowerCase());
            } else if(type == "cluster") {
              zoomToFeatures(feature.get("features"));
            }
          }
        });

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
          popoverAction : Handlebars.compile($('#popoverAction\\.mustache').html()),
          popoverCountry : Handlebars.compile($('#popoverCountry\\.mustache').html()),
          popoverOrganization : Handlebars.compile($('#popoverOrganization\\.mustache').html()),
          popoverPerson : Handlebars.compile($('#popoverPerson\\.mustache').html()),
          popoverService : Handlebars.compile($('#popoverService\\.mustache').html()),
          popoverEvent : Handlebars.compile($('#popoverEvent\\.mustache').html())
        };

      });

      // Defer until vector source is loaded
      if (countryVectorSource.getFeatureById("US")) { // Is this a relieable test?
        my.initialized.resolve();
      } else {
        var listener = countryVectorSource.on('change', function(e) {
          if (countryVectorSource.getState() == 'ready') {
            ol.Observable.unByKey(listener);
            my.initialized.resolve();
          }
        });
      }

    },

    layout : function() {
      if(world) { // without this condition -> error on loading single resource
        world.updateSize();
      }
    },

    attach : function(context) {

      // Populate map with pins from resource listings
      markers = {};
      $('[data-behaviour~="populateMap"]', context).each(function(){
        var json = JSON.parse( $(this).find('script[type="application/ld+json"]').html() );
        var _markers = [];
        if(json instanceof Array) {
          for (i in json) {
            _markers = _markers.concat( getMarkers(json[i]) );
          }
        } else {
          _markers = getMarkers(json, Hijax.behaviours.map.getResourceLabel);
        }
        console.log( _markers );
        addPlacemarks( _markers );
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
              clusters[i].setStyle(styles.placemark_cluster.hover(clusters[i]));
            }

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
          }

        });
      });

      // Add heat map data
      $('form#form-resource-filter', context).add($('#country-statistics', context)).each(function(){
        var json = JSON.parse( $(this).find('script[type="application/ld+json"]').html() );
        setAggregations( json );
        if( $(this).is('table') ) {
          $(this).hide();
        }
      });

      // Set zoom
      $('[data-behaviour="map"]', context).each(function() {
        // zoom to bounding box, if focus is set
        setBoundingBox(this);
      });

    },

    initialized : new $.Deferred()

  };

  Hijax.behaviours.map = my;

  return Hijax;

})(jQuery, Hijax);
