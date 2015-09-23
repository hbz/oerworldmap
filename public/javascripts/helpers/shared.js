var console = console || {
  log: function(message) {
    java.lang.System.out.println(JSON.stringify(message, null, 2));
  }
}

Handlebars.registerHelper('getField', function (string, options) {
  var parts = string.split('.');
  return parts[parts.length -1];
});

Handlebars.registerHelper('getIcon', function (string, options) {
  var icons = {
    'service': 'desktop',
    'person': 'user',
    'organization': 'users',
    'article': 'comment',
    'action': 'gears'
  };
  return new Handlebars.SafeString('<i class="fa fa-' + (icons[string.toLowerCase()] || 'desktop') + '"></i>');
});

Handlebars.registerHelper('json', function (obj, options) {
  return new Handlebars.SafeString(JSON.stringify(obj, null, 2));
});

Handlebars.registerHelper('getBundle', function (field, options) {
  var bundles = {
    'availableLanguage': 'languages',
    'addressCountry': 'countries'
  }
  return bundles[field] || 'messages';
});

/**
 * Adopted from http://stackoverflow.com/questions/7261318/svg-chart-generation-in-javascript
 */
  Handlebars.registerHelper('pieChart', function(aggregation, options) {

  // FIXME actually an array is passed in , but rhino does not recognize it as such?
  var buckets = [];
  for (bucket in aggregation['buckets']) {
    buckets.push(aggregation['buckets'][bucket]);
  }

  var width = options.hash.width || 400;
  var height = options.hash.height || 400;


  function openTag(type, closing, attr) {
      var html = ['<' + type];
      for (var prop in attr) {
        // A falsy value is used to remove the attribute.
        // EG: attr[false] to remove, attr['false'] to add
        if (attr[prop]) {
          html.push(prop + '="' + attr[prop] + '"');
        }
      }
      return html.join(' ') + (!closing ? ' /' : '') + '>';
    }

  function closeTag(type) {
    return '</' + type + '>';
  }

  function createElement(type, closing, attr, contents) {
    return openTag(type, closing, attr) + (closing ? (contents || '') + closeTag(type) : '');
  }

  var total = buckets.reduce(function (accu, that) {
    return that['doc_count'] + accu;
  }, 0);

  var sectorAngleArr = buckets.map(function (v) { return 360 * v['doc_count'] / total; });

  var arcs = [];
  var startAngle = 0;
  var endAngle = 0;
  for (var i=0; i<sectorAngleArr.length; i++) {
    startAngle = endAngle;
    endAngle = startAngle + sectorAngleArr[i];

    var x1,x2,y1,y2 ;

    x1 = parseInt(Math.round((width/2) + ((width/2)*.975)*Math.cos(Math.PI*startAngle/180)));
    y1 = parseInt(Math.round((height/2) + ((height/2)*.975)*Math.sin(Math.PI*startAngle/180)));

    x2 = parseInt(Math.round((width/2) + ((width/2)*.975)*Math.cos(Math.PI*endAngle/180)));
    y2 = parseInt(Math.round((height/2) + ((height/2)*.975)*Math.sin(Math.PI*endAngle/180)));

    var d = "M" + (width/2) + "," + (height/2)+ "  L" + x1 + "," + y1 + "  A" + ((width/2)*.975) + "," + ((height/2)*.975) + " 0 " +
        ((endAngle-startAngle > 180) ? 1 : 0) + ",1 " + x2 + "," + y2 + " z";

    var c = parseInt(i / sectorAngleArr.length * 360);
    var path = createElement("path", true, {d: d, fill: "hsl(" + c + ", 66%, 50%)"});//,
        //"<title>" + buckets[i]['key'] + " (" + buckets[i]['doc_count'] + ")</title>"
    //);
    var arc = createElement("a", true, {
      "xlink:href": "/resource/?filter.about.@type=" + buckets[i]['key'],
      "xlink:title": buckets[i]['key'] + " (" + buckets[i]['doc_count'] + ")"
    }, path);
    arcs.push(arc);
  }
  return new Handlebars.SafeString(createElement("svg" , true, {
    width: width,
    height: height,
    xmlns: "http://www.w3.org/2000/svg",
    "xmlns:xlink": "http://www.w3.org/1999/xlink"
  }, arcs.join("")));

});
