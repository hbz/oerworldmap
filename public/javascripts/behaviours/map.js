var Hijax = (function ($, Hijax) {

  var iconCodes = {
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
      subNationalVectorSource = null,
      subNationalVectorLayer = null,
      placemarksVectorSource = null,
      placemarksVectorLayer = new ol.layer.Vector({
        style: styles.placemark.base
      }),
      mapboxTileSource = null,
      mapboxTileLayer = null,
      hoveredCountriesOverlay = null,

      standartMapSize = [896, 655],
      standartInitialZoom = 1.85,
      standartMinZoom = 1.85,
      standartMaxZoom = 15,

      defaultCenter = [0, 5000000],
      currentCenter = false,

      current_pins = [],
      current_highlights = [],

      templates = {},

      hoverState = {
        id : false,
        persistent : false
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
      maxZoom: standartMaxZoom
    };
  }

  function getFeatureType(feature) {
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

  function updateHoverState(pixel, eventType) {

    if(hoverState.persistent) {
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

    // get feature type and so on
    if(features.length) {

      var popoverType = getPopoverType( features );

      if (popoverType == 'placemark') {
        features = features.filter(function(feature) {
          return (getFeatureType(feature) == "placemark");
        });
      }

      var show_popover = true;
      var ids = "";

      for (var i = 0; i < features.length; i++) {
        ids += features[i].getId();
      }
    }

    // the statemachine ...
    if(
      ! hoverState.ids &&
      features.length
    ) {
      // ... nothing hovered yet, but now

      if(show_popover) {
        $(popoverElement).show();
        setPopoverContent( features, popoverType, eventType );
        setPopoverPosition( features, popoverType, pixel );
      }
      setFeaturesStyle( features, 'hover' );
      hoverState.ids = ids;
      hoverState.features = features;
      hoverState.popoverType = popoverType;
      world.getTarget().style.cursor = 'pointer';

    } else if(
      hoverState.ids &&
      features.length &&
      ids == hoverState.ids
    ) {
      // ... same features were hovered

      if(show_popover) {
        setPopoverContent( features, popoverType, eventType );
        setPopoverPosition( features, popoverType, pixel );
      }

    } else if(
      hoverState.ids &&
      features.length &&
      ids != hoverState.ids
    ) {
      // ... other features were hovered

      if(show_popover) {
        $(popoverElement).show();
        setPopoverContent( features, popoverType, eventType );
        setPopoverPosition( features, popoverType, pixel );
      } else {
        $(popoverElement).hide();
      }
      resetFeaturesStyle( hoverState.features );
      setFeaturesStyle( features, 'hover' );

      hoverState.ids = ids;
      hoverState.features = features;
      hoverState.popoverType = popoverType;

    } else if(
      hoverState.ids &&
      ! features.length
    ) {
      // ... features were hovered but now none is

      $(popoverElement).hide();
      resetFeaturesStyle( hoverState.features );
      hoverState.ids = false;
      hoverState.features = false;
      hoverState.popoverType = false;
      world.getTarget().style.cursor = '';

    } else {

      // ... do nothing probably – or did i miss somehting?

    }

    // FIXME @j0hj0h: this should probably go somewhere else
    if (world.getView().getZoom() > 8 && (popoverType == "country")) {
      $(popoverElement).hide();
      hoveredCountriesOverlay.setVisible(false);
      world.getTarget().style.cursor = '';
    } else if (show_popover) {
      if(! hoverState.persistent) {
        $(popoverElement).show();
      }
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
      styles[ "placemark" ][ style ]
    );
  }

  function setFeaturesStyle( features, style ) {
    for (var i = 0; i < features.length; i++) {
      setFeatureStyle(features[i], style);
    }
  }

  function resetFeatureStyle( feature ) {
    var feature_type = getFeatureType( feature );

    if( feature_type == 'placemark') {
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

  function setPopoverContent(features, type, eventType) {

    eventType = eventType || 'hover';

    if( type == "placemark" ) {
      //features = features.map(function(feature) { return getFeatureType(feature) == 'placemark' ? feature : null } );
      var content = '';
      var total_resources = features.length;
      var zoom_values = getZoomValues();
      if(total_resources < 6 || eventType == 'click') {
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

        log.debug('MAP setBoundingBox – set center to user:', window.user_location);

        var country_extent = countryVectorSource
          .getFeatureById( window.user_location )
          .getGeometry()
          .getExtent();
        var country_center_x = country_extent[0] + (country_extent[2] - country_extent[0]) / 2;
        var center = [country_center_x, defaultCenter[1]]
        world.getView().setCenter(center);

        // Get zoom values adapted to map size

        log.debug('MAP setBoundingBox – set zoom to initial');

        var zoom_values = getZoomValues();
        world.getView().setZoom(zoom_values.initialZoom);

        currentCenter = center;
      }

      layouted.resolve();
    }
  }

  function zoomToFeatures(features, cluster, pixel) {
    var extent = ol.extent.createEmpty();
    for(var i = 0; i < features.length; i++) {
      ol.extent.extend(extent, features[i].getGeometry().getExtent());
    }
    world.getView().fit(extent, world.getSize(), {
      minResolution: 2
    });
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

    if(hoverState.persistent) {
      // persistent popover – just close it

      hoverState.persistent = false;
      $('#map').removeClass('popover-persistent');
      $(popoverElement).hide();

    } else if(hoverState.popoverType == "country") {
      // it's a country – link to it if zoom is not to high, otherwise do nothing

      if(world.getView().getZoom() < 9) {
        page("/country/" + hoverState.features[0].getId().toLowerCase());
      }

    } else if(hoverState.features.length == 1) {
      // just one feature – link to it ...

      Hijax.behaviours.app.linkToFragment( features[0].getProperties()['resource']['@id'] );

    } else if(hoverState.features.length < 7 || world.getView().getZoom() == getZoomValues().maxZoom) {
      // less then 7 or maximum zoom level – show stacked popover for all hovered features ...

      hoverState.persistent = true;
      $('#map').addClass('popover-persistent');
      setPopoverContent(hoverState.features, 'placemark');
      setPopoverPosition(hoverState.features, 'placemark', pixel);

    } else {
      // more then 7 and we can zoom – let's do so ...

      zoomToFeatures(hoverState.features);
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
        layers: [countryVectorLayer, mapboxTileLayer, hoveredCountriesOverlay, placemarksVectorLayer],
        target: container,
        view: view,
        controls: ol.control.defaults({ attribution: false })
      });

      // bind events

      world.on('pointermove', onPointermove);
      world.on('click', onClick);

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
        if(hoverState.features && ! hoverState.persistent) {
          $(popoverElement).hide();
          resetFeaturesStyle( hoverState.features );
          hoverState.ids = false;
          hoverState.features = false;
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

  function layout() {

    $.when.apply(null, my.attached).done(function(){

      log.debug('MAP layout started (waited to be attached therefor)');

      // clear eventually persistent popover
      $('#map').removeClass('popover-persistent');
      $(popoverElement).hide();
      hoverState.persistent = false;

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

      // check if the behaviours layouted (created at the beginning of attach) is resolved already
      // if so create a local one, otherwise pass the behaviours one ...
      var layouted;
      if(my.layouted.state() == 'resolved') {
        layouted = new $.Deferred();
      } else {
        layouted = my.layouted;
      }
      setBoundingBox($('[data-behaviour="map"]')[0], layouted);

      layouted.done(function(){
        log.debug('MAP layout finished');
      });

    });

    world.updateSize();

  }

  function attach(context, attached) {

    // creating layouted deferred at the beginning of attached, to schedule actions from inside attach
    // to happen after subsequent layout

    my.layouted = $.Deferred();

    // Hide entries not in current map extent by examining all markers (including "indirect" ones)
    // for each list entry
    $('[data-behaviour~="geoFilteredList"]', context).each(function(){

      var container = $(this);

      var checked = sessionStorage.getItem('geoFilteredList') == 'true' ? 'checked="checked"' : '';
      var enabled = $('<input type="checkbox" name="enabled" ' + checked + ' />').change(function() {
        sessionStorage.setItem('geoFilteredList', this.checked);
        restrictListToExtent();
      });

      my.layouted.done(function(){
        restrictListToExtent();
      });

      container.find('.geo-filtered-list-control').prepend($('<label> Search as I move the map</label>').prepend(enabled));
    });

    // Populate pin highlights

    $('[data-behaviour~="populateMapHightlights"]', context).each(function(){
      var json = JSON.parse( $(this).find('script[type="application/ld+json"]').html() );
      //TODO: re-implement
    });

    // Link list entries to pins
    $('[data-behaviour~="linkedListEntries"]', context).each(function(){

      $( this ).on("mouseenter", "li[about]", function() {
        var id = $(this).attr("about");
        var feature = placemarksVectorSource.getFeatureById(id);
        if (feature) {
          feature.setStyle(styles.placemark.hover);
        }
      });

      $( this ).on("mouseleave", "li[about]", function() {
        var id = $(this).attr("about");
        var feature = placemarksVectorSource.getFeatureById(id);
        if (feature) {
          feature.setStyle(styles.placemark.base);
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

  function clearHighlights() {
    current_highlights = [];
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

  function setPlacemarksVectorSource(url) {
    var parser = document.createElement('a');
    parser.href = url;
    var source;
    if (parser.pathname.indexOf("/country/") == 0) {
      source = "/resource.geojson?filter.about.location.address.addressCountry="
        + parser.pathname.split("/")[2].toUpperCase();
    } else {
      source = parser.pathname.slice(0, -1) + ".geojson" + parser.search;
    }
    placemarksVectorSource = new ol.source.Vector({
      url: source,
      format: new ol.format.GeoJSON(),
      wrapX: true
    });
    placemarksVectorLayer.setSource(placemarksVectorSource);
  }

  var my = {

    init : init,
    attach : attach,
    layout : layout,

    initialized : new $.Deferred(),
    layouted : false,
    attached : [],

    clearHighlights : clearHighlights,
    debugShowCountry : debugShowCountry,
    setPlacemarksVectorSource : setPlacemarksVectorSource

  };

  Hijax.behaviours.map = my;

  return Hijax;

})(jQuery, Hijax);
