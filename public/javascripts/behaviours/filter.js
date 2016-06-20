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
    ) { console.log("yes it's active");
      return true;
    } else {
      return false;
    }
  }

  function create_bloodhound(aggregation) {

    // prepare source array

    var bloodhound_source = [];

    for(i in aggregation.buckets) {
      bloodhound_source.push({
        id : aggregation.buckets[i].key,
        label_x : get_label(aggregation.buckets[i].key, aggregation.name), // just label doesn't work for some reason
        count : aggregation.buckets[i].doc_count,
        active : is_active(aggregation.buckets[i].key, aggregation.name)
      });
    }

    bloodhound_source.sort(function(a,b) {
      if(a.label_x < b.label_x) return -1;
      if(a.label_x == b.label_x) return 0;
      if(a.label_x > b.label_x) return 1;
    });

    // create bloodhound

    bloodhounds[ aggregation.name ] = new Bloodhound({
      datumTokenizer : function(d){
        return Bloodhound.tokenizers.whitespace(d.label_x);
      },
      queryTokenizer : Bloodhound.tokenizers.whitespace,
      local : bloodhound_source,
      identify : function(result){
        return result.id;
      },
    });

  }

  function init_typeahead(name) {

    var filter = $('.filter[data-filter-name="' + name + '"]');
    var dropdown = $(filter).find('.dropdown-menu')
    var dropdown_button = $(filter).find('.dropdown-toggle');
    var dropdown_parent = dropdown.parent();
    var typeahead = $(filter).find('.typeahead');

    // prevent dropdown from being closed on click

    dropdown.on('click', function(){
      return false; // dirty
    });

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
      console.log('suggestion', suggestion, e);
      $(e.target).closest('.filter').find('[data-filter-value="' + suggestion.id + '"]').toggleClass('active');

      e.preventDefault();

      //return false;


      //$('[data-filter-name]')
      // dropdown_button.dropdown('toggle');
      // typeahead.typeahead('val', '');
      // my.setLanguage(one, suggestion.id);
    });

    typeahead.bind('typeahead:close', function(e) {
      e.preventDefault();

      // update checkboxes

      var filter = $(e.target).closest('.filter');
      var filter_name = filter.data('filter-name');

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

      // deferr
      my.initialized.resolve();
    },

    attach : function(context) {

      setTimeout(function(){
      $('#filter-container:not(.attached)', context).each(function(){

        var container = this;

        $(container).addClass('attached'); // dirty fix. find out why otherwise filters are attached twice (only happens when filter is set)

        filters = JSON.parse( $(this).find('#json-filters').html() );
        aggregations = JSON.parse( $(this).find('#json-aggregations').html() );

        console.log('filters', filters);
        console.log('aggregations', aggregations);

        // prepare types

        var active_type = ( typeof filters['about.@type'] != 'undefined' ? filters['about.@type'][0] : false );

        for(t in resource_types) {
          resource_types[ t ].active = (active_type == t);
        }

        // prepare aggregations

        for(a in aggregations) {
          // copy identifier to property to have unified access in templates
          aggregations[ a ].name = a;
          // use typeahead?
          if(aggregations[ a ].buckets.length > 20) {
            aggregations[ a ].typeahead = true;
            create_bloodhound(aggregations[ a ]);
          } else {
            aggregations[ a ].typeahead = false;
          }
          // active ?
          aggregations[ a ].active = (typeof filters[ a ] !== 'undefined');
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
          aggregations[ a ].column = i + 1;
          i = (i + 1) % 3;
        }

        // render template

        $(container).prepend(
          templates.filter({
            filters : filters,
            aggregations : aggregations,
            resource_types : resource_types,
            country_aggregation : country_aggregation
          })
        );

        // init typeaheads

        setTimeout(function(){ // without timeout filters aren't in dom yet
          for(name in bloodhounds) {
            init_typeahead(name);
          }
        }, 0);

        // bind toggle filter

        $(container).find('[data-filter-action="toggle-filter"]').click(function(){ console.log('toggle-filter', this);
          if(! $(this).hasClass('active')) {
            return;
          }

          $(this).removeClass('active');
          var filter_name = $(this).closest('.filter').data('filter-name');
          var checkboxes = $('[name="filter.' + filter_name + '"]');
          checkboxes.prop("checked", false);

          $('#form-resource-filter').submit();
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

      });
      }, 1000);
    },

    initialized : new $.Deferred(),

  };

  Hijax.behaviours.filter = my;

  return Hijax;

})(jQuery, Hijax, page);
