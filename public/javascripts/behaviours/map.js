var Hijax = (function ($, Hijax) {

  var

    iconCodes = {
      'organizations': 'users',
      'users': 'user',
      'stories': 'comment'
    },

    colors = {
      'blue-darker': '#2d567b',
      'blue-darker-transparent': [45, 86, 123, 0.5],
      'orange': '#fe8a00',
      'orange-transparent': [254, 138, 0, 0.5]
    },

    styles = {
      placemark : {

        base : new ol.style.Style({
          image: new ol.style.Circle({
            radius: 4,
            stroke: new ol.style.Stroke({
              color: '#fff',
              width: 1
            }),
            fill: new ol.style.Fill({
              color: colors["blue-darker-transparent"]
            })
          }),
          zIndex: 1
        }),

        hover : new ol.style.Style({
          image: new ol.style.Circle({
            radius: 7,
            stroke: new ol.style.Stroke({
              color: '#fff',
              width: 1
            }),
            fill: new ol.style.Fill({
              color: colors["orange-transparent"]
            })
          }),
          zIndex: 2
        }),

        highlight : new ol.style.Style({
          image: new ol.style.Circle({
            radius: 9,
            stroke: new ol.style.Stroke({
              color: '#fff',
              width: 1.5
            }),
            fill: new ol.style.Fill({
              color: colors["orange"]
            })
          }),
          zIndex: 3
        })

      },
    },

    container = null,
    world = null,
    view = null,
    projection = null,

    popover,
    popoverElement,

    countryVectorSource = null,
    countryVectorLayer = null,
    countryVectorLayerHovered = null,

    placemarksVectorSource = null,
    placemarksVectorLayer = new ol.layer.Vector({
      style: styles.placemark.base
    }),

    mapboxTileSource = null,
    mapboxTileLayer = null,

    standartMapSize = [896, 655],
    standartInitialZoom = 1.85,
    standartMinZoom = 1.85,
    standartMaxZoom = 15,

    defaultCenter = [0, 5000000],

    maxPopoverStackSize = 6,

    templates = {},

    state = {
      hover : {
        ids : '',
        features : [],
        popoverType : '' // country, placemark
      },
      scope : 'world',
      highlights : [],
      viewChange : false, // world, country, placemarks, highlights
      persistentPopover : false
    }
  ;


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
      maxZoom: standartMaxZoom
    };
  }


  function getFeatureType(feature) {
    if(! feature) {
      log.error('MAP getFeatureType – invalid feature', feature);
      return;
    }
    if(feature.getId().indexOf("urn:uuid") == 0) {
      return "placemark";
    } else {
      return 'country';
    }
  }


  function getFeatureTypeById(id) {
    if(id.length == 2) {
      return "country";
    } else {
      return "placemark";
    }
  }


  function getPopoverType(features) {
    for (var i = 0; i < features.length; i++) {
      if (getFeatureType(features[i]) == "placemark") {
        return "placemark";
      }
    }
    return "country";
  }


  function updateHoverState(pixel) {

    if(state.persistentPopover) {
      return;
    }

    // get unique features at pixel
    var features = [];
    var processed = [];
    world.forEachFeatureAtPixel(pixel, function(feature, layer) {
      if (processed.indexOf(feature.getId()) == -1) {
        processed.push(feature.getId());
        features.push(feature);
      }
    });


    if(features.length) {
      var popoverType = getPopoverType( features );

      // sort out countries if popover style is placemark
      if (popoverType == 'placemark') {
        features = features.filter(function(feature) {
          return (getFeatureType(feature) == "placemark");
        });
      }

      // unset features, if zoomlevel is to high
      if (world.getView().getZoom() > 8 && (popoverType == "country")) {
        features = [];
      }

      // create ids-string
      var ids = "";
      for (var i = 0; i < features.length; i++) {
        ids += features[i].getId();
      }
    }

    // the statemachine ...
    if(
      ! state.hover.ids &&
      features.length
    ) {
      // ... nothing hovered yet, but now

      $(popoverElement).show();
      setPopoverContent(features, popoverType);
      setPopoverPosition(features, popoverType, pixel);
      setFeaturesStyle(features, 'hover' );

      state.hover.ids = ids;
      state.hover.features = features;
      state.hover.popoverType = popoverType;
      world.getTarget().style.cursor = 'pointer';

    } else if(
      state.hover.ids &&
      features.length &&
      ids == state.hover.ids
    ) {
      // ... same features were hovered

      setPopoverPosition(features, popoverType, pixel );

    } else if(
      state.hover.ids &&
      features.length &&
      ids != state.hover.ids
    ) {
      // ... other features were hovered

      $(popoverElement).show();
      setPopoverContent(features, popoverType);
      setPopoverPosition(features, popoverType, pixel);
      resetFeaturesStyle(state.hover.features);
      setFeaturesStyle(features, 'hover');

      state.hover.ids = ids;
      state.hover.features = features;
      state.hover.popoverType = popoverType;

    } else if(
      state.hover.ids &&
      ! features.length
    ) {
      // ... features were hovered but now none is

      $(popoverElement).hide();
      resetFeaturesStyle(state.hover.features);
      state.hover.ids = false;
      state.hover.features = false;
      state.hover.popoverType = false;
      world.getTarget().style.cursor = '';

    } else {

      // ... do nothing probably – or did i miss somehting?

    }
  }


  function setFeatureStyle(feature, style) {
    var feature_type = getFeatureType(feature);

    if(feature_type == 'country') {

      if(style == 'hover') {
        countryVectorLayerHovered.getSource().addFeature(feature);
      } else if(style == 'base') {
        countryVectorLayerHovered.getSource().removeFeature(feature);
      }

    } else if(feature_type == 'placemark') {

      if(state.highlights.indexOf(feature.getId()) !== -1) {
        feature.setStyle(styles['placemark']['highlight']);
      } else {
        feature.setStyle(styles['placemark'][ style ]);
      }

    }
  }


  function setFeaturesStyle(features, style) {
    for (var i = 0; i < features.length; i++) {
      setFeatureStyle(features[i], style);
    }
  }


  function resetFeatureStyle(feature) {
    setFeatureStyle(feature, 'base');
  }


  function resetFeaturesStyle( features ) {
    for (var i = 0; i < features.length; i++) {
      resetFeatureStyle(features[i]);
    }
  }


  function setPopoverPosition(features, type, pixel) {

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
      var popup_coord = features[0].getGeometry().getCoordinates();
      popup_coord[0] += (offset * 20037508.34 * 2);

      popover.setPosition(popup_coord);
      popover.setOffset([0, -30]);
    }
  }


  function setPopoverContent(features, type) {
    if( type == "placemark" ) {
      var content = '';
      var total_resources = features.length;
      var zoom_values = getZoomValues();
      if(total_resources <= maxPopoverStackSize) {
        for(var i = 0; i < features.length; i++) {
          var properties = features[i].getProperties();
          content += templates['popoverResource']({ resource: properties });
        }
      } else {
        var aggregation = {};
        for(var i = 0; i < features.length; i++) {
          var properties = features[i].getProperties();
          aggregation[ properties["@type"] ] = aggregation[ properties["@type"] ] + 1 || 1;
        }
        content += templates['popoverCluster'](aggregation);
      }

    } else if( type == "country") {
      var properties = features[0].getProperties();

      // setup empty countrydata, if undefined
      if( typeof properties.country == 'undefined' ) {
        properties.country = {
          key : features[0].getId()
        }
      }

      var country = properties.country;
      country.key = country.key.toUpperCase();
      var content = templates.popoverCountry(country);
    }

    $(popoverElement).find('.popover-content').html(content);

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


  function userCenterWorld() {

    // set center
    var user_country_extent = countryVectorSource
      .getFeatureById( window.user_location )
      .getGeometry()
      .getExtent();
    var user_country_center_x = user_country_extent[0] + (user_country_extent[2] - user_country_extent[0]) / 2;
    var center = [user_country_center_x, defaultCenter[1]]
    world.getView().setCenter(center);

    // set zoom
    var zoom_values = getZoomValues();
    world.getView().setZoom(zoom_values.initialZoom);
  }


  function updateHighlights() {
    log.debug('MAP updateHighlights', state.highlights);
    placemarksVectorLayer.setStyle(styles.placemark.base);

    if(! state.highlights || state.highlights.length == 0) {
      return;
    }

    for(var i = 0; i < state.highlights.length; i++) {
      setFeatureStyle(
        placemarksVectorSource.getFeatureById(
          state.highlights[i]
        )
      , 'highlight');
    }
  }


  function zoomToFeatures(features) {
    if(! features || features.length == 0) {
      log.debug('MAP zoomToFeatures – empty set, doing nothing', features);
      return;
    }
    if(! Array.isArray(features)) {
      log.error('MAP zoomToFeatures – features must be an array');
      return;
    }
    var extent = ol.extent.createEmpty();
    for(var i = 0; i < features.length; i++) {
      ol.extent.extend(extent, features[i].getGeometry().getExtent());
    }
    if(extent[0] == Infinity) {
      log.error('MAP zoomToFeatures – extent is infinity', extent, features);
    }
    world.getView().fit(extent, world.getSize(), {
      minResolution: 2
    });
  }


  function getFeaturesById(source, ids) {
    features = [];
    for(var i = 0; i < ids.length; i++) {
      features.push(
        source.getFeatureById(ids[i])
      );
    }
    return features;
  }


  function updateBoundingBox(layouted) {

    switch (state.viewChange) {

      case 'world':
        log.debug('MAP updateBoundingBox – set center to user:', window.user_location);
        userCenterWorld();
        break;

      case 'country':
        log.debug('MAP updateBoundingBox – focus country', state.scope);
        zoomToFeatures(
          [countryVectorSource.getFeatureById(state.scope)]
        );
        break;

      case 'placemarks':
        log.debug('MAP updateBoundingBox – focus placemarks');
        zoomToFeatures(
          placemarksVectorSource.getFeatures()
        );
        break;

      case 'highlights':
        log.debug('MAP updateBoundingBox – focus highlights', state.highlights);
        zoomToFeatures(
          getFeaturesById(placemarksVectorSource, state.highlights)
        );
        break;

      case false:
        log.debug('MAP updateBoundingBox – no view change');
        break;
    }

    state.viewChange = false;
    layouted.resolve(); // probably not needed anymore because no async calls left
  }


  function onPointermove(evt) {
    if (evt.dragging) { return; }
    var pixel = world.getEventPixel(evt.originalEvent);
    updateHoverState(pixel);
  }


  // #features == 1 : link
  // #features 2 - 6 : persistent
  // #features > 6 : zoom

  function onClick(evt) {

    var pixel = world.getEventPixel(evt.originalEvent);
    updateHoverState(pixel);

    log.debug('MAP onClick – state:', state);

    if(state.persistentPopover) {
      // persistent popover – just close it
      log.debug('MAP onClick – close persistent popover');

      state.persistentPopover = false;
      $('#map').removeClass('popover-persistent');
      $(popoverElement).hide();

    } else if(state.hover.popoverType == false) {
      // water or country in high zoom level
      log.debug('MAP onClick – do nothing');

    } else if(state.hover.popoverType == "country") {
      // it's a country – link to it if zoom is not to high, otherwise do nothing
      log.debug('MAP onClick – page to country, if zoom is less then 9');

      if(world.getView().getZoom() < 9) {
        page("/country/" + state.hover.features[0].getId().toLowerCase());
      }

    } else if(state.hover.features.length == 1) {
      // just one feature – link to it ...
      log.debug('MAP onClick – page to resource');

      Hijax.behaviours.app.linkToFragment( state.hover.features[0].getId() );

    } else if(state.hover.features.length <= maxPopoverStackSize || world.getView().getZoom() == getZoomValues().maxZoom) {
      // less then 7 or maximum zoom level – show stacked persistent popover for all hovered features ...
      log.debug('MAP onClick – show persistent popover');

      state.persistentPopover = true;
      $('#map').addClass('popover-persistent');
      setPopoverContent(state.hover.features, 'placemark');
      setPopoverPosition(state.hover.features, 'placemark', pixel);

    } else {
      // more then 7 and we can zoom – let's do so ...
      log.debug('MAP onClick – zoom to features');

      zoomToFeatures(state.hover.features);
      setTimeout(function(){
        updateHoverState(pixel);
      }, 300);

    }
  }


  /* --- public functions --- */


  function init(context) {

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

      // Map container

      container = $('<div id="map"></div>')[0];
      $(this).prepend(container);

      // Country vector layer

      countryVectorLayer = new ol.layer.Vector({
        title: 'country',
        source: countryVectorSource
      });

      setCountryVectorSource();

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
      countryVectorLayerHovered = new ol.layer.Vector({
        title : 'country-hover',
        source : new ol.source.Vector({
          features : collection,
          useSpatialIndex : false // optional, might improve performance
        }),
        style : function(feature, resolution) {
          return [new ol.style.Style({
            stroke : new ol.style.Stroke({
              color : '#0c75bf',
              width : world.getView().getZoom() > 4 ? 2 : 1.5
            })
          })];
        },
        updateWhileAnimating: false, // optional, for instant visual feedback
        updateWhileInteracting: false // optional, for instant visual feedback
      });

      // Map object

      world = new ol.Map({
        layers : [countryVectorLayer, mapboxTileLayer, countryVectorLayerHovered, placemarksVectorLayer],
        target : container,
        view : view,
        controls : ol.control.defaults({ attribution: false })
      });

      // bind events

      world.on('pointermove', onPointermove);
      world.on('click', onClick);

      // init popover

      popoverElement = $('<div class="popover fade top in layout-typo-small" role="tooltip"><div class="arrow"></div><div class="popover-content"></div></div>')[0];
      popover = new ol.Overlay({
        element : popoverElement,
        positioning : 'bottom-center',
        stopEvent : false,
        wrapX : true
      });

      world.addOverlay(popover);

      $(container).mouseleave(function(){
        if(state.hover.features && ! state.persistentPopover) {
          $(popoverElement).hide();
          resetFeaturesStyle( state.hover.features );
          state.hover.ids = false;
          state.hover.features = false;
        }
      });

      // precompile handlebar templates

      templates = {
        popoverResource : Handlebars.compile($('#popoverResource\\.mustache').html()),
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

  }


  function attach(context, attached) {

    // creating layouted deferred at the beginning of attached, to schedule actions from inside attach
    // to happen after subsequent layout
    my.layouted = $.Deferred();

    // Populate pin highlights
    $('[data-behaviour~="populateMapHighlights"]', context).each(function(){
      var id = $(this).attr("about");
      setHighlights([id]);
    });

    // Link list entries to pins
    $('[data-behaviour~="linkedListEntries"]', context).each(function(){

      $( this ).on("mouseenter", "li[about]", function() {
        var id = $(this).attr("about");
        var feature = placemarksVectorSource.getFeatureById(id);
        if (feature) {
          setFeatureStyle(feature, 'hover');
        }
      });

      $( this ).on("mouseleave", "li[about]", function() {
        var id = $(this).attr("about");
        var feature = placemarksVectorSource.getFeatureById(id);
        if (feature) {
          resetFeatureStyle(feature);
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

    attached.resolve();

  }


  function layout() {

    $.when.apply(null, my.attached).done(function(){

      log.debug('MAP layout started (waited to be attached therefor)');

      // clear eventually persistent popover
      $('#map').removeClass('popover-persistent');
      $(popoverElement).hide();
      state.persistentPopover = false;

      // check if the behaviours layouted (created at the beginning of attach) is resolved already
      // if so create a local one, otherwise pass the behaviours one ...
      var layouted;
      if(my.layouted.state() == 'resolved') {
        layouted = new $.Deferred();
      } else {
        layouted = my.layouted;
      }

      $.when(countrySourceLoaded, placemarksSourceLoaded).done(function(){
        setTimeout(function(){
          updateHighlights();
          updateBoundingBox(layouted);
        }, 0);
      });

      layouted.done(function(){
        log.debug('MAP layout finished');
      });

    });

    world.updateSize();

  }


  // not public, just here to be next to setPlacemarksVectorSource
  function setCountryVectorSource() {
    countrySourceLoaded = $.Deferred();

    countryVectorSource = new ol.source.Vector({
      wrapX : true,
      loader : function(){
        var url = '/assets/json/ne_50m_admin_0_countries_topo.json';
        var format = new ol.format.TopoJSON();
        var source = this;

        $.getJSON(url, '', function(response){
          if(Object.keys(response).length > 0){
            var features = format.readFeatures(response, {
              featureProjection: 'EPSG:3857'
            });
            source.addFeatures(features);
            countrySourceLoaded.resolve();
          }
        });
      }
    });

    countryVectorLayer.setSource(countryVectorSource);
  }


  function setPlacemarksVectorSource(url) {
    placemarksSourceLoaded = $.Deferred();

    var parser = document.createElement('a');
    parser.href = url;
    var url_geojson;
    if (parser.pathname.indexOf("/country/") == 0) {
      url_geojson = "/resource.geojson?filter.about.location.address.addressCountry="
        + parser.pathname.split("/")[2].toUpperCase();
    } else {
      url_geojson = parser.pathname.slice(0, -1) + ".geojson" + parser.search;
    }

    placemarksVectorSource = new ol.source.Vector({
      wrapX : true,
      loader : function(){
        var format = new ol.format.GeoJSON();
        var source = this;

        $.getJSON(url_geojson, '', function(response){
          if(Object.keys(response).length > 0){
            var features = format.readFeatures(response, {
              featureProjection: 'EPSG:3857'
            });
            source.addFeatures(features);
            placemarksSourceLoaded.resolve();
          }
        });
      }
    });

    placemarksVectorLayer.setSource(placemarksVectorSource);
  }


  function setScope(scope) {
    state.scope = scope;
  }


  function setHighlights(ids) {
    var old_highlights = getFeaturesById(placemarksVectorSource, state.highlights);
    console.log('old_highlights', old_highlights);
    state.highlights = ids;
    resetFeaturesStyle(old_highlights);
  }


  // world, country, feature, features
  function scheduleViewChange(viewChange) {
    state.viewChange = viewChange;
  }


  function debugShowCountry(id) {
    var feature = countryVectorSource.getFeatureById(id);

    feature.setStyle([new ol.style.Style({
      fill: new ol.style.Fill({
        color: '#ff0000'
      })
    })]);

    var extent = ol.extent.createEmpty();
    ol.extent.extend(extent, feature.getGeometry().getExtent());
    world.getView().fit(extent, world.getSize(), {
      minResolution: 2
    });
  }


  var my = {

    init : init,
    attach : attach,
    layout : layout,

    initialized : new $.Deferred(),
    layouted : false,
    attached : [],

    placemarksSourceLoaded : new $.Deferred(),
    countrySourceLoaded : new $.Deferred(),

    setPlacemarksVectorSource : setPlacemarksVectorSource,

    setScope : setScope,
    setHighlights : setHighlights,
    scheduleViewChange : scheduleViewChange,

    debugShowCountry : debugShowCountry

  };


  Hijax.behaviours.map = my;

  return Hijax;

})(jQuery, Hijax);
