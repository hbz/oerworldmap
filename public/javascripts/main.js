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
  
  var map = $('#worldmap');
  
  var nUsersByCountry = {};
  
  $.getJSON(pathCountryDataJson, function(data){
    for(key in data) {
      if(Math.random() > 0.5) {
        nUsersByCountry[ data[key]["alpha-2"] ] = getRandomInt(0, 10);
      } else {
        nUsersByCountry[ data[key]["alpha-2"] ] = 0;
      }
    }
    
    map.vectorMap({
      backgroundColor: $('body').css('background-color'),
      zoomButtons: false,
      zoomOnScroll: false,
      series: {
        regions: [{
          values: nUsersByCountry,
          scale: ['#FFFFFF', '#CD533B'], // #CD533B
          normalizeFunction: 'linear'
        }]
      },
      onRegionTipShow: function(e, el, code){
        el.html('<strong>' + nUsersByCountry[code] + '</strong> users registered in ' + el.html());
      }
    });
  });	
	
});
