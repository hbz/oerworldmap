/*

  Types of Filters:
  - Type
  - Country
  -

*/

var Hijax = (function ($, Hijax, page) {

  var templates = {
    filter : Handlebars.compile($('#filter\\.mustache').html())
  };

  var resource_types = {
    'Organization' : { 'icon' : 'users' },
    'Person' : { 'icon' : 'user' },
    'Service' : { 'icon' : 'desktop' },
    'Project' : { 'icon' : 'gears' },
    'Article' : { 'icon' : 'comment' },
    'Event' : { 'icon' : 'calendar' }
  };

  var my = {

    init : function(context) {

      // deferr
      my.initialized.resolve();
    },

    attach : function(context) {

      $('#filter-container', context).each(function(){

        var container = this;

        var filters = JSON.parse( $(this).find('#json-filters').html() );
        var aggregations = JSON.parse( $(this).find('#json-aggregations').html() );

        console.log('filters', filters);
        console.log('aggregations', aggregations);

        if( typeof filters['about.@type'] != 'undefined' ) {
          resource_types[ filters['about.@type'][0] ].active = true;
        }

        // render template

        $(this).prepend(
          templates.filter({
            // filters : filters,
            resource_types : resource_types,
          })
        );

        // bind actions

        $(this).find('[data-filter-action="set-type"]').click(function(){
          $(this).siblings().removeClass('active');
          $(this).toggleClass('active');
          var active = $(this).hasClass('active');

          var checkboxes = $(container).find('[name="filter.about.@type"]');
          checkboxes.prop("checked", false);

          var checkbox = $(container).find('[name="filter.about.@type"][value="' + $(this).data('filter-value') + '"]');
          checkbox.prop("checked", active);
        });

      });
    },

    initialized : new $.Deferred(),

  };

  Hijax.behaviours.filter = my;

  return Hijax;

})(jQuery, Hijax, page);
