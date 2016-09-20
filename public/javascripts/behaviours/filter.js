/*

  Types of Filters:
  - Type
  - Country
  -

*/

var Hijax = (function ($, Hijax, page) {

  var filters = {};
  var aggregations = {};

  var templates = {
    filter : Handlebars.compile($('#filter\\.mustache').html()),
    suggestion : Handlebars.compile($('#filter-dropdown-entry\\.mustache').html())
  };

  Handlebars.registerPartial('filter-dropdown', $('#filter-dropdown\\.mustache').html());
  Handlebars.registerPartial('filter-dropdown-entry', $('#filter-dropdown-entry\\.mustache').html());

  var bloodhounds = {};

  var resource_types = {
    'Organization' : { 'icon' : 'users' },
    'Person' : { 'icon' : 'user' },
    'Service' : { 'icon' : 'desktop' },
    'Action' : { 'icon' : 'gears' },
    'Article' : { 'icon' : 'comment' },
    'Event' : { 'icon' : 'calendar' }
  };

  var i18n_bundles = {
    "about.availableChannel.availableLanguage" : "languages",
    "about.location.address.addressCountry" : "countries"
  };

  function get_label(key, aggregation_name) {
    return $('[name="filter.' + aggregation_name + '"][value="' + key + '"]').siblings('.label').text();
  }

  function is_active(key, aggregation_name) {
    if(
      typeof filters[ aggregation_name ] !== 'undefined' &&
      filters[ aggregation_name ].indexOf( key ) > -1
    ) {
      return true;
    } else {
      return false;
    }
  }

  function localize(aggregation, key) {
    if(i18n_bundles[ aggregation ]) {
      var bundle = i18n_bundles[ aggregation ];
    } else {
      var bundle = 'messages';
    }

    if(bundle == "countries") {
      key = key.toUpperCase();
    }

    return i18nStrings[ bundle ][ key ];
  }

  function get_field(string) {
    var parts = string.split('.');
    var field = parts[parts.length -1];
    if (field == "@id") {
      field = parts[parts.length -2];
    }
    return field;
  };

  function get_option_label(name, value) {
    var text = $('[name="filter.' + name + '"][value="' + value + '"]').siblings('.label').text();
    if(text) {
      return text;
    } else {
      // fallback:
      // get_option_label doesn't work when options are set, where no hits exists for,
      // because then the corresponding filteroptions aren't in the server side form
      return localize(name, value);
    }
  }

  function get_filter_label(name) {
    return $('h2[data-filter-name="' + name + '"]').text();
  }

  function pimp_aggregation(aggregation, name) {

    // copy identifier to property to have unified access in templates
    aggregation.name = name;

    // show ?
    if(aggregation.buckets.length) {
      aggregation.show = true;
    } else {
      aggregation.show = false;
      return;
    }

    // active ?
    aggregation.active = (typeof filters[ name ] !== 'undefined');

    // button title
    if(aggregation.active) {
      var parts = [];
      for(var i = 0; i < filters[ name ].length; i++) {
        parts.push( get_option_label(name, filters[ name ][ i ]) );
      }
      parts.sort();
      aggregation.filter_options = parts;
      aggregation.button_title = get_filter_label(name) + ': ' + aggregation.filter_options.join(", ");
    } else {
      aggregation.button_title = "Filter by " + get_filter_label(name);
    }

    // button text
    if(aggregation.active) {
      aggregation.button_text = aggregation.filter_options[0] + (aggregation.filter_options.length > 1 ? ', ...' : '');
    } else {
      aggregation.button_text = localize('messages', get_field(name));
    }

    // pimp buckets
    for(var i = 0; i < aggregation.buckets.length; i++) {
      aggregation.buckets[ i ].id = aggregation.buckets[ i ].key;
      aggregation.buckets[ i ].label_x = get_label(aggregation.buckets[ i ].key, name); // just label doesn't work for some reason
      aggregation.buckets[ i ].active = is_active(aggregation.buckets[ i ].key, name);
    }

    // sort buckets
    aggregation.buckets.sort(function(a,b) {
      if(a.label_x < b.label_x) return -1;
      if(a.label_x == b.label_x) return 0;
      if(a.label_x > b.label_x) return 1;
    });

    // use typeahead?
    if(aggregation.buckets.length > 20) {
      aggregation.typeahead = true;
      create_bloodhound(aggregation);
    } else {
      aggregation.typeahead = false;
    }
  }

  function create_bloodhound(aggregation) {
    bloodhounds[ aggregation.name ] = new Bloodhound({
      datumTokenizer : function(d){
        return Bloodhound.tokenizers.whitespace(d.label_x);
      },
      queryTokenizer : Bloodhound.tokenizers.whitespace,
      local : aggregation.buckets,
      identify : function(result){
        return result.id;
      },
    });
  }

  function init_filter(aggregation) {

    var filter = $('.filter[data-filter-name="' + aggregation.name + '"]');
    var dropdown = $(filter).find('.dropdown-menu')
    var dropdown_button = $(filter).find('.dropdown-toggle');
    var dropdown_parent = dropdown.parent();

    // prevent dropdown from being closed on click

    dropdown.on('click', function(){
      return false; // dirty
    });

    if(aggregation.typeahead) {

      init_typeahead(aggregation.name, filter, dropdown, dropdown_button, dropdown_parent);

    } else {

      filter.find('.tt-suggestion').click(function(){
        $(this).find('[data-filter-value]').toggleClass('active');
        filter.addClass('updated');
      });

      dropdown_parent.on('hide.bs.dropdown', function(){
        // if not updated, return
        if(! filter.hasClass('updated')) {
          return;
        } else {
          filter.removeClass('updated');
        }

        filter.find('.button').first().addClass('active');
        update_checkboxes(filter);
        $('#form-resource-filter').submit();
      });

    }
  }

  function init_typeahead(name, filter, dropdown, dropdown_button, dropdown_parent) {

    var typeahead = $(filter).find('.typeahead');

    // init typeahead

    typeahead.typeahead({
      hint : false,
      highlight : true,
      minLength : 0
    },{
      // name: 'languages',
      limit : 9999,
      //display : 'label',
      source : function(q, sync){
        if (q === '') {
          sync( bloodhounds[ name ].local );
        } else {
          bloodhounds[ name ].search(q, sync);
        }
      },
      templates : {
        suggestion : templates['suggestion']
      }
    });

    // focus typeahead on dropdown + hack to initially open the typeahead suggestion list

    dropdown_parent.on('shown.bs.dropdown', function(){
      typeahead.focus();
      typeahead.typeahead('val', 'x');
      typeahead.typeahead('val', '');
    });

    // when selection is made ...

    typeahead.bind('typeahead:beforeselect', function(e, suggestion) {
      var filter = $(e.target).closest('.filter');
      filter.addClass('updated');
      filter.find('[data-filter-value="' + suggestion.id + '"]').toggleClass('active');
      e.preventDefault();
    });

    typeahead.bind('typeahead:close', function(e) {
      e.preventDefault();

      var filter = $(e.target).closest('.filter');
      var filter_name = filter.data('filter-name');

      // if not updated, return

      if(! filter.hasClass('updated')) {
        return;
      } else {
        filter.removeClass('updated');
      }

      // update checkboxes

      update_checkboxes(filter);

      // set button active

      filter.find('.button').first().addClass('active');

      // unset input

      typeahead.typeahead('val', '');

      // submit

      $('#form-resource-filter').submit();
    });
  }

  function update_checkboxes(filter) {
    var filter_name = $(filter).data('filter-name');
    filter.find('[data-filter-value]').each(function(){
      var active = $(this).hasClass('active');
      var checkbox = $('[name="filter.' + filter_name + '"][value="' + $(this).data('filter-value') + '"]');
      checkbox.prop("checked", active);
    });
  }

  var my = {

    init : function(context) {
      my.initialized.resolve();
    },

    attach : function(context) {

      setTimeout(function(){
      $('#filter-container:not(.attached)', context).each(function(){

        var container = this;

        $(container).addClass('attached'); // dirty fix. find out why otherwise filters are attached twice (only happens when filter is set)

        filters = JSON.parse( $(this).find('#json-filters').html() );
        aggregations = JSON.parse( $(this).find('#json-aggregations').html() );

        // console.log('filters', filters);
        // console.log('aggregations', aggregations);

        // prepare types

        var active_type = ( typeof filters['about.@type'] != 'undefined' ? filters['about.@type'][0] : false );

        for(t in resource_types) {
          resource_types[ t ].active = (active_type == t);
        }

        // prepare aggregations

        for(a in aggregations) {
          pimp_aggregation(aggregations[ a ], a);
        }

        // extract country

        var country_aggregation = aggregations['about.location.address.addressCountry'];
        country_aggregation.column = 3;

        // remove special treated aggregations

        delete aggregations['about.location.address.addressCountry'];
        delete aggregations['about.@type'];

        // set columns

        var i = 0;
        for(a in aggregations) {
          if(aggregations[ a ].show) {
            aggregations[ a ].column = i + 1;
            i = (i + 1) % 3;
          }
        }

        // set clear filter position

        var clear_filter_offset = (2 - i) * 4;

        // render template

        $(container).prepend(
          templates.filter({
            filters : filters,
            q : $('[name="q"]').val(),
            aggregations : aggregations,
            resource_types : resource_types,
            country_aggregation : country_aggregation,
            clear_filter_offset : clear_filter_offset
          })
        );

        // init typeaheads

        setTimeout(function(){ // without timeout filters aren't in dom yet
          init_filter(country_aggregation);
          for(name in aggregations) {
            init_filter(aggregations[ name ]);
          }
        }, 0);

        // bind toggle filter buttons

        $(container).find('[data-filter-action="toggle-filter"]').click(function(){
          if(! $(this).hasClass('active')) {
            return;
          }

          $(this).removeClass('active');
          var filter_name = $(this).closest('.filter').data('filter-name');
          var checkboxes = $('[name="filter.' + filter_name + '"]');
          checkboxes.prop("checked", false);

          $('#form-resource-filter').submit();
        });

        // bind apply filter

        $(container).find('[data-filter-action="apply-filter"]').click(function(e){
          var dropdown_button = $(e.target).closest('.dropdown').find('[data-toggle="dropdown"]');
          dropdown_button.dropdown('toggle');
        });

        // bind type filter

        $(container).find('[data-filter-action="set-type"]').click(function(){
          $(this).siblings().removeClass('active');
          $(this).toggleClass('active');
          var active = $(this).hasClass('active');

          var checkboxes = $(container).find('[name="filter.about.@type"]');
          checkboxes.prop("checked", false);

          var checkbox = $(container).find('[name="filter.about.@type"][value="' + $(this).data('filter-value') + '"]');
          checkbox.prop("checked", active);

          $('#form-resource-filter').submit();
        });

        // bind text search

        $('[data-filter-action="search"]').click(function(){
          $('[name="q"]').val( $('[data-filter-name="q"]').val() );
          $('#form-resource-filter').submit();
        });

        $('[data-filter-name="q"]').keypress(function(e) {
          if(e.which == 13) {
            $('[name="q"]').val( $(this).val() );
            $('#form-resource-filter').submit();
          }
        });

      });
      }, 0);
    },

    initialized : new $.Deferred(),

  };

  Hijax.behaviours.filter = my;

  return Hijax;

})(jQuery, Hijax, page);
