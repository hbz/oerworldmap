var Hijax = (function ($, Hijax) {

  var my = {

    templates : {},

    attach : function(context) {

      my.templates['widget'] = Handlebars.compile($('#place_widget\\.mustache').html());

      // prepare countries array

      my.countries_array = [];

      for(i in i18nStrings.countries) {
        my.countries_array.push({
          id: i,
          label: i18nStrings.countries[i]
        });
      }

      my.countries_array.sort(function(a,b) {
        if(a.label < b.label) return -1;
        if(a.label == b.label) return 0;
        if(a.label > b.label) return 1;
      });

      my.countries_array.unshift({id: null, label: "None"});

      // init countries bloodhound

      my.countries_bloodhoud = new Bloodhound({
        datumTokenizer: function(d){
          return Bloodhound.tokenizers.whitespace(d.label);
        },
        queryTokenizer: Bloodhound.tokenizers.whitespace,
        local: my.countries_array,
        identify: function(result){
          return result.id;
        },
      });

      // iterate over widgets

      $('[data-behaviour~="place"]', context)
        .not('[data-dont-behave] [data-behaviour~="place"]')
        .each(function()
      {

        var widget = $(this);

        widget.addClass('behaving');

        var inputs = {
          'location[address][streetAddress]' : null,
          'location[address][addressLocality]' : null,
          'location[address][postalCode]' : null,
          'location[address][addressRegion]' : null,
          'location[address][addressCountry]' : null,
          'location[geo][lat]' : null,
          'location[geo][lon]' : null
        };

        var placeholders = {
          'location[address][streetAddress]' : 'Address',
          'location[address][addressLocality]' : 'City',
          'location[address][postalCode]' : 'ZIP',
          'location[address][addressRegion]' : 'Province',
          'location[address][addressCountry]' : 'Country Code (DE, US, GB, ... to be replaced by dropdown)',
          'location[geo][lat]' : 'Latitude',
          'location[geo][lon]' : 'Longitude'
        }

        for(name in inputs) {
          var dashed_name = name.replace('[', '-').replace('][', '-').replace(']', ''); // identifiers with [ don't work in handlebars
          inputs[ dashed_name ] = widget
            .find('[name$="' + name + '"]')
            .addClass('form-control')
            .attr('placeholder', placeholders[ name ])
            .attr('title', placeholders[ name ])
            .detach()[0].outerHTML;
        }

        // fill widget template

        widget.append(
          $(my.templates['widget']( inputs ))
        );


        // init country typeahead

        var country_typeahead = widget.find('#country-typeahead');
        var country_dropdown = widget.find('#country-dropdown');
        var country_dropdown_button = country_dropdown.find('button');
        var country_dropdown_menu = country_dropdown.find('.dropdown-menu');

        var current_country_code = widget.find('[name="location[address][addressCountry]"]').val();

        if(current_country_code != "") {
          country_dropdown_button.find('.text').text(
            i18nStrings.countries[ current_country_code ]
          );
        }

        country_dropdown_menu.on('click', function(){
          return false; // dirty
        });

        country_typeahead.typeahead({
          hint: false,
          highlight: true,
          minLength: 0
        },{
          name: 'countries',
          limit: 9999,
          display: 'label',
          source: function(q, sync){
            if (q === '') {
              sync(my.countries_array);
            } else {
              my.countries_bloodhoud.search(q, sync);
            }
          }
        });

        // hack to initially open the typeahead suggestion list

        country_dropdown.on('shown.bs.dropdown', function(){
          country_typeahead.focus();
          country_typeahead.typeahead('val', 'x');
          country_typeahead.typeahead('val', '');
        });

        // when selection is made ...

        country_typeahead.bind('typeahead:select', function(e, suggestion) {
          country_dropdown_button.dropdown('toggle');
          country_typeahead.typeahead('val', '');
          widget.find('[name="location[address][addressCountry]"]').val(
            suggestion.id
          );
          country_dropdown_button.find('.text').text(
            suggestion.label
          );
        });


        // add map; FIXME: timeout due to map canvas initialized with display: none

        window.setTimeout(function() {

          // init location lookup typeahead
          var location_typeahead = widget.find('#location-typeahead');
          var location_dropdown = widget.find('#location-dropdown');
          var location_dropdown_button = location_dropdown.find('button');
          var location_dropdown_menu = location_dropdown.find('.dropdown-menu');

          location_dropdown_menu.on('click', function() {
            return false; // dirty
          });

          function debounce(fn, delay) {
            var timer = null;
            return function () {
              var context = this, args = arguments;
              clearTimeout(timer);
              timer = setTimeout(function () {
                      fn.apply(context, args);
              }, delay);
            };
          }

          location_typeahead.typeahead({
            hint: false,
            highlight: true,
            minLength: 0
          },{
            name: 'locations',
            limit: 9999,
            display: 'label',
            source: debounce(function(q, sync, async) {
              if (q !== '') {
                $.get('https://search.mapzen.com/v1/autocomplete?api_key=search-2bvcBc8&text=' + q, function(data) {
                  var results = [];
                  for (var i = 0; i < data.features.length; i++) {
                    results.push({
                      label: data.features[i].properties.label,
                      feature: data.features[i]
                    })
                  }
                  async(results);
                });
              }
            }, 250)
          });

          // hack to initially open the typeahead suggestion list

          location_dropdown.on('shown.bs.dropdown', function(){
            location_typeahead.focus();
            location_typeahead.typeahead('val', 'x');
            location_typeahead.typeahead('val', '');
          });

          location_typeahead.bind('typeahead:select', function(e, suggestion) {

            location_dropdown_button.dropdown('toggle');
            location_typeahead.typeahead('val', '');

            // set source of marker vector layer to new vector source with selected feature
            markers.setSource(new ol.source.Vector({
                features: (new ol.format.GeoJSON()).readFeatures(suggestion.feature, { featureProjection: 'EPSG:3857' })
              })
            );

            // process feature in marker source
            markers.getSource().forEachFeature(function(feature){
              // style feature
              feature.setStyle(style);
              // get code from region layer by feature coordinates
              setRegionFromFeature(feature);
              map.addInteraction(createDragInteraction(feature));
            });

            // fit view
            map.getView().fit(markers.getSource().getExtent(), map.getSize(), {
              minResolution: 2
            });

            // populate remaining form inputs
            var properties = suggestion.feature.properties;
            widget.find('[name="location[address][streetAddress]"]').val(properties.name || '');
            widget.find('[name="location[address][postalCode]"]').val(properties.postalcode || '');
            widget.find('[name="location[address][addressLocality]"]').val(properties.locality || '');
            widget.find('[name="location[address][addressCountry]"]').val(properties.country_a.substring(0, 2));
            setCoordinatesFromLonLat(suggestion.feature.geometry.coordinates)
            country_dropdown_button.find('.text').text(i18nStrings.countries[properties.country_a.substring(0, 2)]);

          });

          function createDragInteraction(feature) {
            var dragInteraction = new ol.interaction.Modify({
              features: new ol.Collection([feature]),
              style: null,
              pixelTolerance: 20
            });
            dragInteraction.on('modifyend',function() {
              setCoordinatesFromLonLat(ol.proj.toLonLat(feature.getGeometry().getCoordinates()));
              setRegionFromFeature(feature);
            }, feature);
            return dragInteraction;
          }

          function setRegionFromFeature(feature) {
            var iso3166_2 = regions.getSource().getFeaturesAtCoordinate(feature.getGeometry().getCoordinates())[0] || null;
            if (iso3166_2) {
              widget.find('[name="location[address][addressRegion]"]').val(iso3166_2.getId());
            } else {
              widget.find('[name="location[address][addressRegion]"]').val("");
            }
          }

          function setCoordinatesFromLonLat(lonLat) {
            widget.find('[name="location[geo][lat]"]').val(lonLat[1] || '');
            widget.find('[name="location[geo][lon]"]').val(lonLat[0] || '');
          }

          // marker style
          var style = new ol.style.Style({
            text: new ol.style.Text({
              text: '\uf041',
              font: 'normal 1.5em FontAwesome',
              textBaseline: 'Bottom',
              fill: new ol.style.Fill({
                color: "#fe8a00"
              }),
              stroke: new ol.style.Stroke({
                color: 'white',
                width: 1
              })
            })
          });

          // layer for marker of current location
          var markers = new ol.layer.Vector({});

          // layer for country regions, needed to retrieve ISO 3166-2 code (region)
          var regions = new ol.layer.Vector({
            source: new ol.source.Vector({
              url: '/assets/json/ne_10m_admin_1_states_provinces_topo.json',
              format: new ol.format.TopoJSON(),
              // noWrap: true,
              wrapX: true
            })
          });

          // OSM layer
          var streets = new ol.layer.Tile({
              source: new ol.source.OSM()
          });

          // the map
          var map = new ol.Map({
            target: 'location-lookup-map',
            interactions: ol.interaction.defaults({mouseWheelZoom:false}),
            layers: [ regions, streets, markers ],
            view: new ol.View({
              center: [0, 5000000],
              zoom: 1
            })
          });

          // mark current location on load
          var lat = parseFloat(widget.find('[name="location[geo][lat]"]').val().trim());
          var lon = parseFloat(widget.find('[name="location[geo][lon]"]').val().trim());
          if (lat && lon) {
            var feature = new ol.Feature({
              geometry: new ol.geom.Point(ol.proj.fromLonLat([lon, lat]))
            });
            feature.setStyle(style);
            markers.setSource(new ol.source.Vector({
              features: [ feature ]
            }));
            // fit view
            map.getView().fit(markers.getSource().getExtent(), map.getSize(), {
              minResolution: 2
            });
            // drag interaction
            map.addInteraction(createDragInteraction(feature));

            // set region code in form input when region vector source is loaded
            if (regions.getSource().getFeatureById("US.CA")) { // Is this a relieable test?
              setRegionFromFeature(feature);
            } else {
              var listener = regions.getSource().on('change', function(e) {
                if (regions.getSource().getState() == 'ready') {
                  setRegionFromFeature(feature);
                }
              });
            }
          }

        }, 500);

      });

    }

  };

  Hijax.behaviours.place = my;
  return Hijax;

})(jQuery, Hijax);
