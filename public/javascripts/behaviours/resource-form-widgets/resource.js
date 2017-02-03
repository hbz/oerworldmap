var Hijax = (function ($, Hijax) {

  var my = {

    templates : {},
    datasets : {},
    bloodhounds : {},
    bloodhoundDefers : {},

    fillDatasetRecursive : function(dataset, scope, depth) {
      var property_name = scope.hasOwnProperty('hasTopConcept') ? 'hasTopConcept' : 'narrower';

      for(var i = 0; i < scope[ property_name ].length; i++) {
        scope[ property_name ][ i ][ 'depth' ] = depth;
        dataset.push( scope[ property_name ][ i ] );
        if( scope[ property_name ][ i ].hasOwnProperty('narrower') ) {
          my.fillDatasetRecursive(dataset, scope[ property_name ][ i ], depth + 1);
        }
      }

    },

    createDataset : function(lookup_url) {
      var dfd = $.Deferred();

      if( my.datasets[ lookup_url ] ) {

        dfd.resolve();

      } else {

  	    return $.ajax({
    	    url: lookup_url,
    	    headers: {
      	    Accept: 'application/json'
    	    },
    	    success: function(data){
            var dataset = [];
      	    my.fillDatasetRecursive(dataset, data, 0);
      	    my.datasets[ lookup_url ] = dataset;
      	  },
      	  error: function(jqXHR, status, error){
        	  console.log('error', jqXHR, status);
      	  }
        });

      }
    },

    createBloodhound : function(lookup_url) {
      var dfd = $.Deferred();

      if( my.bloodhounds[lookup_url] ) {

        dfd.resolve();

      } else {

          if( lookup_url.endsWith('json') ) {

            $.when(
              my.createDataset( lookup_url )
            ).then(function(){

              my.bloodhounds[ lookup_url ] = new Bloodhound({
                datumTokenizer: function(d){
                  return Bloodhound.tokenizers.whitespace(
                    bloodhoundAccentFolding.normalize(
                      d.name[0]['@value']
                    )
                  );
                },
                queryTokenizer: bloodhoundAccentFolding.queryTokenizer,
                local: my.datasets[ lookup_url ],
                identify: function(result){
                  return result['@id'];
                }
              });

              dfd.resolve();

            });

          } else {

            my.bloodhounds[ lookup_url ] = new Bloodhound({
              datumTokenizer: function(d){
                var token_parts = [];

                if( typeof d.name !== 'undefined' ) {
                  token_parts.push( d.name[0]['@value'] );
                }

                if( typeof d.alternateName !== 'undefined' ) {
                  token_parts.push( d.alternateName[0]['@value'] );
                }

                if( token_parts.length == 0 ) {
                  token_parts.push( d['@id'] );
                }

                return Bloodhound.tokenizers.whitespace(
                  bloodhoundAccentFolding.normalize(
                    token_parts.join(' ')
                  )
                );
              },
              queryTokenizer: bloodhoundAccentFolding.queryTokenizer,
              initialize: false,
              prefetch: {
                url: lookup_url,
                cache: false,
                prepare: function(settings){
                  settings.headers = {
              	    Accept: 'application/json'
            	    };
                  return settings;
                },
                transform: function(response) {
                  return response.member;
                }
              },
              identify: function(result){
                return result['@id'];
              },
            });

            my.bloodhounds[ lookup_url ].clearPrefetchCache()
            my.bloodhounds[ lookup_url ].initialize();

            $(window).focus(function(){
              my.bloodhounds[ lookup_url ].clearPrefetchCache()
              my.bloodhounds[ lookup_url ].initialize(true);
            });

            dfd.resolve();

          }
      }

      return dfd.promise();
    },

    gotBloodhound : function(lookup_url) {
      if( ! my.bloodhoundDefers[ lookup_url ] ) {
        my.bloodhoundDefers[ lookup_url ] = my.createBloodhound( lookup_url );
      }
      return my.bloodhoundDefers[ lookup_url ];
    },

    getAllowedTypes : function(lookup_url, title) {
      if( my.isVocabulary(lookup_url) ) {
        return [title];
      } else {
        return $.map(
          lookup_url.match(/@type[=:](\w*)/g),
          function(val){
            return val.replace(/@type[:=]/, '');
          }
        );
      }
    },

    isVocabulary : function(lookup_url) {
      if( lookup_url.endsWith('json') ) {
        return true;
      } else {
        return false;
      }
    },

    attach : function(context) {

      // my.getBloodhound('/assets/json/esc.json');

      my.templates['item'] = Handlebars.compile($('#resource_item\\.mustache').html());
      my.templates['multiple-one'] = Handlebars.compile($('#resource_multiple-one\\.mustache').html());
      my.templates['typeahead'] = Handlebars.compile($('#resource_typeahead\\.mustache').html());
      my.templates['typeahead-dropdown'] = Handlebars.compile($('#resource_typeahead-dropdown\\.mustache').html());
      my.templates['typeahead-suggestion'] = Handlebars.compile($('#resource_typeahead-suggestion\\.mustache').html());

      // iterate over widgets

      $('[data-behaviour~="resource"]', context)
        .not('[data-dont-behave] [data-behaviour~="resource"]')
        .each(function()
      {

        var widget = $(this);
        var lookup_url = $(this).data('lookup');
        var key = $(this).data('key');
        var title = widget.find('h4').text();
        var allowed_types = my.getAllowedTypes(lookup_url, title);
        var is_vocabulary = my.isVocabulary(lookup_url);

        // prevent paging

        if( ! is_vocabulary ) {
          lookup_url = lookup_url + '&size=9999';
        }

        // mark behaving for styling

        $(this).addClass('behaving');

        // remove the last empty one

        widget.find('.multiple-one').filter(function() {
          return $(this).find('input').get(0).value == "";
        }).last().remove();

        // init each

        widget.find('.multiple-one').each(function(){
          my.initOne(this);
        });

        // append the add control

        widget.append(
          my.templates[ is_vocabulary ? 'typeahead-dropdown' : 'typeahead' ]({
            allowed_types : allowed_types.csOr()
          })
        );

        var typeahead = widget.find('.typeahead');

        if( is_vocabulary ) {
          var dropdown = widget.find('.dropdown');
          var dropdown_button = dropdown.find('button');
          var dropdown_menu = dropdown.find('.dropdown-menu');
        }

        // prevent dropdown from being closed on click

        if( is_vocabulary ) {
          dropdown_menu.on('click', function(){
            return false; // dirty
          });
        }

        // init typeahead

        $.when(
          my.gotBloodhound( lookup_url )
        ).then(function(){

          typeahead.typeahead({
            hint: false,
            highlight: true,
            minLength: 0
          },{
            name: 'xyz',
            limit: 9999,
            source: function(q, sync, async){
              if (q === '') {
                sync(my.datasets[ lookup_url ]);
              } else {
                my.bloodhounds[ lookup_url ].search(q, sync, async);
              }
            },
            templates: {
              suggestion: my.templates['typeahead-suggestion']
            }
          });

        });

        // hack to initially open the typeahead suggestion list

        if( is_vocabulary ) {
          dropdown.on('shown.bs.dropdown', function(){
            typeahead.focus();
            typeahead.typeahead('val', 'x');
            typeahead.typeahead('val', '');
          });
        }

        // add and remove tt-open on input also, to make it stylable correspondingly

        typeahead.bind('typeahead:open', function(e, suggestions) {
          if($(this).parent().find('.tt-suggestion').length) {
            $(this).addClass('tt-open');
          }
        });

        typeahead.bind('typeahead:close', function() {
          $(this).removeClass('tt-open');
        });

        typeahead.bind('typeahead:render', function(e, suggestions) {
          if($(this).parent().find('.tt-suggestion').length) {
            $(this).addClass('tt-open');
          } else {
            $(this).removeClass('tt-open');
          }
        });

        // when selection is made ...

        typeahead.bind('typeahead:select', function(e, suggestion) {
          if( is_vocabulary ) {
            dropdown_button.dropdown('toggle');
          }

          typeahead.typeahead('val', '');

          if( widget.find('.multiple-one').length ) {
            var highest_index = parseInt(
              _(
                widget
                  .find('.multiple-one').last().find('input').attr('name')
                  .match(/\[(\d+)\]/)
              ).last()
            , 10); //console.log( highest_index );
          } else {
            var highest_index = 0;
          }

          var max_cardinality = parseInt(widget.find('.multiple-list').attr("data-max-cardinality"));
          if (max_cardinality <= highest_index) {
            return;
          }

          suggestion['key'] = key;
          suggestion['@index'] = highest_index + 1;

          var one_new = $( my.templates['multiple-one']( suggestion ) );

          widget.find('.multiple-list').append( one_new );

          my.initOne( one_new );
        });

      });

    },

    initOne : function(one) {

      // reorganize layout

      var raw_json = $(one).find('script').html();

      if(raw_json) {
        var json_data = JSON.parse( raw_json );

        $(one).append(
          my.templates['item']( json_data )
        );

        $(one).find('[data-action="delete"]').click(function(){
          $(one).fadeOut(function(){
            $(this).remove();
          });
        })
      }

    },

    attached : []
  };

  Hijax.behaviours.resource = my;
  return Hijax;

})(jQuery, Hijax);
