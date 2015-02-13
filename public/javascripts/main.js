// --- helpers ---

// Returns a random integer between min (included) and max (excluded)
// Using Math.round() will give you a non-uniform distribution!
function getRandomInt(min, max) {
  return Math.floor(Math.random() * (max - min)) + min;
}


$(document).ready(function(){
	
  // --- visions ---
  
  $('.vision-statements').slick({
    infinite: true,
    dots: true,
    autoplay: true,
    autoplaySpeed: 8000,
    arrows: false
  });
  
  
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
  
  data = {
    "DE" : 15,
    "CH" : 4,
    "AT" : 6,
    "GB" : 12,
    "FR" : 9,
    "ES" : 5,
    "US" : 9,
    "PL" : 2,
    "BF" : 1,
    "NO" : 5,
    "CN" : 6,
    "ID" : 4,
    "GH" : 4,
    "IR" : 5,
    "BR" : 7,
    "CD" : 5,
    "KZ" : 9,
    "RU" : 2,
    "RO" : 4,
    "DZ" : 3,
    "CA" : 2
  };
  
  map.vectorMap({
    backgroundColor: '#0c75bf',
    zoomButtons: false,
    zoomOnScroll: false,
    series: {
      regions: [{
        values: data,
        scale: ['#ffffff', '#a1cd3f'],
        normalizeFunction: 'linear'
      }]
    },
    onRegionTipShow: function(e, el, code){
      if(typeof data[code] != 'undefined') {
        el.html(
          '<strong>' + data[code] + '</strong> users registered in ' + el.html() + '<br>' +
          'Click to register ...'
        );
      } else {
        el.html(
          'No users registered in ' + el.html() + '<br>' +
          'Click to register ...'
        );
      }
    },
    onRegionClick: function(e, code) {
      console.log(code);
      $('select[name="address.addressCountry"]').val(code);
      $('html, body').animate({
  			scrollTop: $('#user-register').offset().top - 100
  		}, 500, function() {
  			if(history.pushState) {
  				history.pushState(null, null, '#user-register');
  			} else {
  				// window.location.hash = link_hash_divided[1];
  			}
  		});

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
      .fail(function(jqXHR) {
        a.replaceWith(hijax(body(jqXHR.responseText)));
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
      .fail(function(jqXHR) {
        form.replaceWith(hijax(body(jqXHR.responseText)));
       });
    return false;
  });

  return element;

}

function body(data) {
  return $(data.match(/<\s*body.*>[\s\S]*<\s*\/body\s*>/ig).join(""))
}

