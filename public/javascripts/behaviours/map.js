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
      osmTileSource = null,
      osmTileLayer = null,
      hoveredCountriesOverlay = null,

      standartMapSize = [896, 655],
      standartInitialZoom = 1.85,
      standartMinZoom = 1.85,
      standartMaxZoom = 8,

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
                  width: 3
                })
              })
            })
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
                  width: 3
                })
              })
            })
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
      var size_vactor = $('#map').height() / standartMapSize[1];
    } else {
      // it's more portrait, so take width
      var size_vactor = $('#map').width() / standartMapSize[0];
    }

    size_vactor = Math.pow(size_vactor, 0.5);

    return {
      initialZoom: standartInitialZoom * size_vactor,
      minZoom: standartMinZoom * size_vactor,
      maxZoom: standartMaxZoom * size_vactor
    };
  }

  function getFeatureType(feature) {
    if( typeof feature == 'object' ) {
      var id = feature.getId();
    } else {
      var id = feature;
    }

    if( id.length !== 2 ) {
      return "placemark";
    } else {
      return "country";
    }
  }

  function updateHoverState(pixel) {

    // get feature at pixel
    var feature = world.forEachFeatureAtPixel(pixel, function(feature, layer) {
      return feature;
    });

    // get feature type
    if(feature) {
      var type = getFeatureType( feature );
    }

    // the statemachine ...
    if(
      ! hoverState.id &&
      feature
    ) {
      // ... no popover yet, but now. show it, update position, content and state

      $(popoverElement).show();
      setPopoverContent( feature, type );
      setPopoverPosition( feature, type, pixel );
      setFeatureStyle( feature, 'hover' );
      hoverState.id = feature.getId();
      world.getTarget().style.cursor = 'pointer';

    } else if(
      hoverState.id &&
      feature &&
      feature.getId() == hoverState.id
    ) {
      // ... popover was active for the same feature, just update position

      setPopoverPosition( feature, type, pixel );

    } else if(
      hoverState.id &&
      feature &&
      feature.getId() != hoverState.id
    ) {
      // ... popover was active for another feature, update position, content and state

      setPopoverContent( feature, type )
      setPopoverPosition( feature, type, pixel );
      setFeatureStyle( feature, 'hover' );
      resetFeatureStyle( hoverState.id );

      hoverState.id = feature.getId();

    } else if(
      hoverState.id &&
      ! feature
    ) {
      // ... popover was active, but now no feature is hovered. hide popover and update state

      $(popoverElement).hide();
      resetFeatureStyle( hoverState.id );
      hoverState.id = false;
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
      styles[ feature_type ][ style ]()
    );
  }

  function resetFeatureStyle( feature_id ) {

    if( getFeatureType(feature_id) == 'placemark' ) {
      setFeatureStyle(
        placemarksVectorSource.getFeatureById( feature_id ),
        'base'
      );
    }

    if( getFeatureType(feature_id) == 'country' ) {
      hoveredCountriesOverlay.getSource().removeFeature(
        countryVectorSource.getFeatureById( feature_id )
      );
    }

  }

  function setPopoverPosition(feature, type, pixel) {

    var coord = world.getCoordinateFromPixel(pixel);

    if( type == 'country' ) {
      popover.setPosition(coord);
      popover.setOffset([0, -20]);
    }

    if( type == 'placemark' ) {
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
    var properties = feature.getProperties();

    if( type == "placemark" ) {
      var content = templates['popover' + properties.type](properties);
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

    for(var j = 0; j < aggregations["by_country"]["buckets"].length; j++) {
      var aggregation = aggregations["by_country"]["buckets"][j];
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

    var focusId = $('[data-view="map"]').attr("data-focus");

    if(
      focusId &&
      getFeatureType(focusId) == "country"
    ) {
      var focused_country = focusId;
    } else {
      var focused_country = false;
    }

    // build heat data hashmap

    for(var j = 0; j < aggregations["by_country"]["buckets"].length; j++) {
      var aggregation = aggregations["by_country"]["buckets"][j];
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
    placemarksVectorSource.addFeatures(placemarks);
  }

  function setBoundingBox(element) {

    var dataFocus = element.getAttribute("data-focus");
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

      // Set extent of map view
      world.getView().fit(boundingBox, world.getSize());


    }

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
        feature.setStyle(styles['placemark']['base']());
        _markers.push(feature);
      }
    }

    if (!markers.length) {
      for (var key in resource) {
        if ('referencedBy' == key) {
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

      if (!$('div[data-view="map"]', context).length) {
        return new $.Deferred();
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

      $('div[data-view="map"]', context).each(function() {

        // move footer to map container
        $('footer').appendTo(this);

        // switch style
        $(this).addClass("map-view");
        $('body').removeClass("layout-scroll").addClass("layout-fixed");

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

        placemarksVectorSource = new ol.source.Vector({
          wrapX: true
        });

        placemarksVectorLayer = new ol.layer.Vector({
          source: placemarksVectorSource
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
              if (4 < view.getZoom()) {
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
          layers: [countryVectorLayer, osmTileLayer, hoveredCountriesOverlay, placemarksVectorLayer],
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
            var properties = feature.getProperties();
            if (properties.url) {
              window.location = properties.url;
            } else {
              window.location = "/country/" + feature.getId().toLowerCase();
            }
          }
        });

        // init popover
        popoverElement = $('<div class="popover fade top in" role="tooltip"><div class="arrow"></div><div class="popover-content"></div></div>')[0];
        popover = new ol.Overlay({
          element: popoverElement,
          positioning: 'bottom-center',
          stopEvent: false,
          wrapX: true
        });
        world.addOverlay(popover);

        // precompile handlebar templates
        templates = {
          popoverAction : Handlebars.compile($('#popoverAction\\.mustache').html()),
          popoverCountry : Handlebars.compile($('#popoverCountry\\.mustache').html()),
          popoverOrganization : Handlebars.compile($('#popoverOrganization\\.mustache').html()),
          popoverPerson : Handlebars.compile($('#popoverPerson\\.mustache').html()),
          popoverService : Handlebars.compile($('#popoverService\\.mustache').html())
        };

      });

      // Defer until vector source is loaded
      var deferred = new $.Deferred();
      if (countryVectorSource.getFeatureById("US")) { // Is this a relieable test?
        deferred.resolve();
      } else {
        var listener = countryVectorSource.on('change', function(e) {
          if (countryVectorSource.getState() == 'ready') {
            ol.Observable.unByKey(listener);
            deferred.resolve();
          }
        });
      }
      return deferred;

    },

    attach : function(context) {

      // Populate map with pins from single resources
      $('article.resource-story', context)
        .add($('div.resource-organization', context))
        .add($('div.resource-action', context))
        .add($('div.resource-service', context))
        .add($('div.resource-person', context))
        .each(function() {
          var json = JSON.parse( $(this).find('script').html() );
          var markers = getMarkers(json, Hijax.behaviours.map.getResourceLabel);
          addPlacemarks( markers );
        });

      // Populate map with pins from resource listings
      // FIXME: don't use class names for js actions -> reorganize behaviours
      $('.populate-map', context).each(function(){
        var json = JSON.parse( $(this).find('script[type="application/ld+json"]').html() );
        var markers = [];
        for (i in json) {
          var markers = markers.concat( getMarkers(json[i]) );
        }
        addPlacemarks( markers );
      });

      // Link list entries to pins
      $('[data-behaviour="linkedListEntries"]', context).each(function(){
        $( this ).on("mouseenter", "li", function() {
          var id = this.getAttribute("about");
          var script = $(this).closest("ul").children('script[type="application/ld+json"]');
          if (script.length) {
            var json = JSON.parse( script.html() );
            var resource = json.filter(function(resource) {
              return resource['@id'] == id;
            })[0];
            var markers = getMarkers(resource);
            for (var i = 0; i < markers.length; i++) {
              markers[i].setStyle(styles.placemark.hover());
            }
          }
        });
        $( this ).on("mouseleave", "li", function() {
          var id = this.getAttribute("about");
          var script = $(this).closest("ul").children('script[type="application/ld+json"]');
          if (script.length) {
            var json = JSON.parse( script.html() );
            var resource = json.filter(function(resource) {
              return resource['@id'] == id;
            })[0];
            var markers = getMarkers(resource);
            for (var i = 0; i < markers.length; i++) {
              markers[i].setStyle(styles.placemark.base());
            }
          }
        });
      });

      // Add heat map data
      $('[about="#users-by-country"]', context).each(function(){
        var json = JSON.parse( $(this).find('script[type="application/ld+json"]').html() );
        setAggregations( json );
        $(this).find('tr').hide();
      });

      // Set zoom
      $('[data-view="map"]', context).each(function() {
        // zoom to bounding box, if focus is set
        setBoundingBox(this);
      });

    }
  };

  Hijax.behaviours.map =  my;
  return Hijax;

})(jQuery, Hijax);
