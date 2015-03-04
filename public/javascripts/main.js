// --- helpers ---

// Returns a random integer between min (included) and max (excluded)
// Using Math.round() will give you a non-uniform distribution!
function getRandomInt(min, max) {
  return Math.floor(Math.random() * (max - min)) + min;
}

String.prototype.cutOff = function(x) {
  //trim the string to the maximum length
  var trimmedString = this.substr(0, x);
  
  //re-trim if we are in the middle of a word
  trimmedString = trimmedString.substr(0, Math.min(trimmedString.length, trimmedString.lastIndexOf(" ")));
  
  return trimmedString + " ...";
};


// --- hijax helper ---

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

  var table = $('table[about="#users-by-country"]'),
      map = $('#worldmap'),
      json = JSON.parse(table.find('script').html()),
      data = {};

  for (i in json.entries) {
    data[json.entries[i].key.toUpperCase()] = json.entries[i].value;
  }

  if(false) {
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
  }

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
      var country_champion = false;
      var users_registered = false;

      if(
        $('ul[about="#country-champions"] li[data-country-code="' + code + '"]').length
      ) {
        country_champion = true;
      }

      if(
        typeof data[code] != 'undefined'
      ) {
        users_registered = true;
      }

      el.html(
        (
          users_registered
          ?
          '<i class="fa fa-fw fa-user"></i> <strong>' + data[code] + '</strong> users registered in ' + el.html() + ' (Click to register ...)<br>'
          :
          '<i class="fa fa-fw fa-user"></i> No users registered in ' + el.html() + ' (Click to register ...)<br>'
        ) + (
          country_champion
          ?
          '<i class="fa fa-fw fa-trophy"></i> And we have a country champion!<br>'
          :
          ''
        )
      );
    },
    onRegionClick: function(e, code) {
      console.log(code);
      $('select[name="workLocation[address][addressCountry]"]').val(code);
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


// --- blog ---

google.load("feeds", "1");

function initialize() {
  var feed = new google.feeds.Feed("https://oerworldmap.wordpress.com/feed/");
  feed.load(function(result) {
    if (!result.error) {
      var latest_post = result.feed.entries[0];
      
      // add 300 character snippet
      latest_post.contentSnippet300 = $( result.feed.entries[0].content ).text().cutOff(300);
      
      // add formated date
      var published_date = new Date( latest_post.publishedDate );
      latest_post.publishedDateFormated = published_date.toLocaleDateString();
      
      // render template
      $.get('/assets/mustache/LandingPage/blog-post-preview.mustache', function(template) {
        var rendered = Mustache.render(template, {post: latest_post});
        $('#blog-link').prepend(rendered);
      });
    }
  });
}
google.setOnLoadCallback(initialize);
