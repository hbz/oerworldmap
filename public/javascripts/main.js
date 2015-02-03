// --- helpers ---

// Returns a random integer between min (included) and max (excluded)
// Using Math.round() will give you a non-uniform distribution!
function getRandomInt(min, max) {
  return Math.floor(Math.random() * (max - min)) + min;
}


$(document).ready(function(){
	
  // --- visions ---
  
  var visions = $('blockquote.vision').hide(),
      i = 0;
  
  (function cycle() {
    visions.eq(i).fadeIn(800).delay(5000).fadeOut(800, cycle);
    i = ++i % visions.length;
  })();
  
  
  // --- map ---
  var table = $('table#users_by_country'),
      map = $('#worldmap'),
      json = JSON.parse(table.find('script').html()),
      data = {};

  for (property in json) {
    if ("@" == property.charAt(0)) {
      continue;
    }
    data[property.toUpperCase()] = json[property];
  }
  map.vectorMap({
    backgroundColor: $('body').css('background-color'),
    zoomButtons: false,
    zoomOnScroll: false,
    series: {
      regions: [{
        values: data,
        scale: ['#cfdfba', '#a1cd3f'],
        normalizeFunction: 'linear'
      }]
    },
    onRegionTipShow: function(e, el, code){
      el.html('<strong>' + data[code] + '</strong> users registered in ' + el.html());
    }
  });
  table.hide()
  
  // --- hijax behavior ---
  hijax($('body'));
	
});

function hijax(element) {

  $('a.hijax.transclude', element).each(function() {
    var a = $(this);
    $.get(a.attr('href'))
      .done(function(data) {
        a.replaceWith(hijax(body(data)));
      })
      .fail(function(data) {
        a.replaceWith("<pre>" + data.responseText + "</pre>");
      });
  });

  $('form', element).submit(function() {
    var form = $(this);
    var action = form.attr('action');
    var method = form.attr('method');
    $.ajax({type: method, url: action, data: form.serialize()})
      .done(function(data) {
        form.replaceWith(hijax(body(data)));
      })
      .fail(function(data) {
        form.replaceWith("<pre>" + data.responseText + "</pre>");
       });
    return false;
  });

  return element;

}

function body(data) {
  return $(data.match(/<\s*body.*>[\s\S]*<\s*\/body\s*>/ig).join(""))
}

