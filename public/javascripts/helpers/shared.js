var console = console || {
  log: function(message) {
    java.lang.System.out.println(JSON.stringify(message, null, 2));
  }
};

if (typeof String.prototype.endsWith !== 'function') {
  String.prototype.endsWith = function(suffix) {
    return this.indexOf(suffix, this.length - suffix.length) !== -1;
  };
}

if (typeof Array.prototype.csOr !== 'function') {
  Array.prototype.csOr = function() {
    if(this.length > 1) {
      return this.slice(0, -1).join(', ') + ' or ' + this[this.length - 1];
    } else {
      return this[0];
    }
  };
}


// From https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/keys
if (!Object.keys) {
  Object.keys = (function() {
    'use strict';
    var hasOwnProperty = Object.prototype.hasOwnProperty,
        hasDontEnumBug = !({ toString: null }).propertyIsEnumerable('toString'),
        dontEnums = [
          'toString',
          'toLocaleString',
          'valueOf',
          'hasOwnProperty',
          'isPrototypeOf',
          'propertyIsEnumerable',
          'constructor'
        ],
        dontEnumsLength = dontEnums.length;

    return function(obj) {
      if (typeof obj !== 'object' && (typeof obj !== 'function' || obj === null)) {
        throw new TypeError('Object.keys called on non-object');
      }

      var result = [], prop, i;

      for (prop in obj) {
        if (hasOwnProperty.call(obj, prop)) {
          result.push(prop);
        }
      }

      if (hasDontEnumBug) {
        for (i = 0; i < dontEnumsLength; i++) {
          if (hasOwnProperty.call(obj, dontEnums[i])) {
            result.push(dontEnums[i]);
          }
        }
      }
      return result;
    };
  }());
}

/**
 * https://github.com/bramstein/url-template
 */
(function (root, factory) {
    if (typeof exports === 'object') {
        module.exports = factory();
    } else if (typeof define === 'function' && define.amd) {
        define([], factory);
    } else {
        root.urltemplate = factory();
    }
}(this, function () {
  /**
   * @constructor
   */
  function UrlTemplate() {
  }

  /**
   * @private
   * @param {string} str
   * @return {string}
   */
  UrlTemplate.prototype.encodeReserved = function (str) {
    return str.split(/(%[0-9A-Fa-f]{2})/g).map(function (part) {
      if (!/%[0-9A-Fa-f]/.test(part)) {
        part = encodeURI(part);
      }
      return part;
    }).join('');
  };

  /**
   * @private
   * @param {string} operator
   * @param {string} value
   * @param {string} key
   * @return {string}
   */
  UrlTemplate.prototype.encodeValue = function (operator, value, key) {
    value = (operator === '+' || operator === '#') ? this.encodeReserved(value) : encodeURIComponent(value);

    if (key) {
      return encodeURIComponent(key) + '=' + value;
    } else {
      return value;
    }
  };

  /**
   * @private
   * @param {*} value
   * @return {boolean}
   */
  UrlTemplate.prototype.isDefined = function (value) {
    return value !== undefined && value !== null;
  };

  /**
   * @private
   * @param {string}
   * @return {boolean}
   */
  UrlTemplate.prototype.isKeyOperator = function (operator) {
    return operator === ';' || operator === '&' || operator === '?';
  };

  /**
   * @private
   * @param {Object} context
   * @param {string} operator
   * @param {string} key
   * @param {string} modifier
   */
  UrlTemplate.prototype.getValues = function (context, operator, key, modifier) {
    var value = context[key],
        result = [];

    if (this.isDefined(value) && value !== '') {
      if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
        value = value.toString();

        if (modifier && modifier !== '*') {
          value = value.substring(0, parseInt(modifier, 10));
        }

        result.push(this.encodeValue(operator, value, this.isKeyOperator(operator) ? key : null));
      } else {
        if (modifier === '*') {
          if (Array.isArray(value)) {
            value.filter(this.isDefined).forEach(function (value) {
              result.push(this.encodeValue(operator, value, this.isKeyOperator(operator) ? key : null));
            }, this);
          } else {
            Object.keys(value).forEach(function (k) {
              if (this.isDefined(value[k])) {
                result.push(this.encodeValue(operator, value[k], k));
              }
            }, this);
          }
        } else {
          var tmp = [];

          if (Array.isArray(value)) {
            value.filter(this.isDefined).forEach(function (value) {
              tmp.push(this.encodeValue(operator, value));
            }, this);
          } else {
            Object.keys(value).forEach(function (k) {
              if (this.isDefined(value[k])) {
                tmp.push(encodeURIComponent(k));
                tmp.push(this.encodeValue(operator, value[k].toString()));
              }
            }, this);
          }

          if (this.isKeyOperator(operator)) {
            result.push(encodeURIComponent(key) + '=' + tmp.join(','));
          } else if (tmp.length !== 0) {
            result.push(tmp.join(','));
          }
        }
      }
    } else {
      if (operator === ';') {
        result.push(encodeURIComponent(key));
      } else if (value === '' && (operator === '&' || operator === '?')) {
        result.push(encodeURIComponent(key) + '=');
      } else if (value === '') {
        result.push('');
      }
    }
    return result;
  };

  /**
   * @param {string} template
   * @return {function(Object):string}
   */
  UrlTemplate.prototype.parse = function (template) {
    var that = this;
    var operators = ['+', '#', '.', '/', ';', '?', '&'];

    return {
      expand: function (context) {
        return decodeURIComponent(template.replace(/\{([^\{\}]+)\}|([^\{\}]+)/g, function (_, expression, literal) {
          if (expression) {
            var operator = null,
                values = [];

            if (operators.indexOf(expression.charAt(0)) !== -1) {
              operator = expression.charAt(0);
              expression = expression.substr(1);
            }

            expression.split(/,/g).forEach(function (variable) {
              var tmp = /([^:\*]*)(?::(\d+)|(\*))?/.exec(variable);
              values.push.apply(values, that.getValues(context, operator, tmp[1], tmp[2] || tmp[3]));
            });

            if (operator && operator !== '+') {
              var separator = ',';

              if (operator === '?') {
                separator = '&';
              } else if (operator !== '#') {
                separator = operator;
              }
              return (values.length !== 0 ? operator : '') + values.join(separator);
            } else {
              return values.join(',');
            }
          } else {
            return that.encodeReserved(literal);
          }
        }));
      }
    };
  };

  return new UrlTemplate();
}));

Handlebars.registerHelper('localized', function(list, options) {
  // Get requested language from Java or JS
  language = java.util.Locale.getDefault() || navigator.language || navigator.userLanguage;
  var result = '';
  // Empty list
  if (!list) {
    return options.inverse(this);
  }
  // Check for entries in requested language
  for (var i = 0; i < list.length; i++) {
    if (list[i]['@language'] == language) {
      result = result + options.fn(list[i]);
    }
  }
  // Requested language not available, default to en
  if (result == '') {
    for (var i = 0; i < list.length; i++) {
      if (list[i]['@language'] == 'en') {
        result = result + options.fn(list[i]);
      }
    }
  }
  // Neither requested language nor en available, return all of first available
  if (result == '') {
    for (var i = 0; i < list.length; i++) {
      if (list[i]['@language'] == list[0]['@language']) {
        result = result + options.fn(list[i]);
      }
    }
  }
  return result;
});

Handlebars.registerHelper('getField', function (string, options) {
  var parts = string.split('.');
  var field = parts[parts.length -1];
  if (field == "@id") {
    field = parts[parts.length -2];
  }
  return field;
});

Handlebars.registerHelper('getIcon', function (string, options) {
  var type = string || "";
  var icons = {
    'service': 'desktop',
    'person': 'user',
    'organization': 'users',
    'article': 'comment',
    'action': 'gears',
    'concept': 'tag',
    'conceptscheme': 'sitemap',
    'event': 'calendar'
  };
  return new Handlebars.SafeString(
    '<i class="fa fa-fw fa-' + (icons[type.toLowerCase()] || 'question') + '"></i>'
  );
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

Handlebars.registerHelper('getResourceUrl', function (url, options) {
  return new Handlebars.SafeString("/resource/" + url);
});

Handlebars.registerHelper('externalLink', function (url, options) {

  var icon = 'fa-external-link-square';

  if (url.indexOf('twitter.com') > -1) {
    icon = 'fa-twitter-square';
  } else if (url.indexOf('facebook.com') > -1) {
    icon = 'fa-facebook-square';
  } else if (url.indexOf('instagram.com') > -1) {
    icon = 'fa-instagram';
  } else if (url.indexOf('linkedin.com') > -1) {
    icon = 'fa-linkedin-square';
  } else if (url.indexOf('youtube.com') > -1) {
    icon = 'fa-youtube-square';
  }

  return new Handlebars.SafeString('<i class="fa fa-fw ' + icon + '"></i><a href="' + url + '">' + url + '</a>');

});

Handlebars.registerHelper('removeFilterLink', function (filter, value, href) {
  var matchFilter = new RegExp("[?&]filter." + filter + "=" + value, "g");
  var matchFrom = new RegExp("from=\\d+", "g");
  return new Handlebars.SafeString(
    href.replace(matchFilter, '').replace(matchFrom, 'from=0')
  );
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

    var c = parseInt((i + 200) / sectorAngleArr.length * 360);
    var path = createElement("path", true, {d: d, fill: "hsl(" + c + ", 66%, 50%)"});

    var href = urltemplate.parse(options.hash['href-template']).expand(buckets[i]);
    var arc = createElement("a", true, {
      "xlink:href": href,
      // FIXME: since we cannot access other javascript helpers via Handlebars.helpers (why?),
      // we are accessing the Java handlebars helpers here for internationalization.
      "xlink:title": Packages.helpers.HandlebarsHelpers._i18n(buckets[i]['key'], null) + " (" + buckets[i]['doc_count'] + ")"
    }, path);
    arcs.push(arc);
  }
  return new Handlebars.SafeString(createElement("svg" , true, {
    width: width,
    height: height,
    class: "chart",
    viewbox: "0 0 " + width + " " + height,
    xmlns: "http://www.w3.org/2000/svg",
    "xmlns:xlink": "http://www.w3.org/1999/xlink"
  }, arcs.join("")));

});


Handlebars.registerHelper('ifObjectNotEmpty', function(obj, options){
  /*

  Here http://stackoverflow.com/a/679937/1060128 this is suggested to be compatible with Pre-ECMA 5 (IE 8)
  But it doesn't work.

  for(var prop in obj) {
    if(obj.hasOwnProperty(prop)) {
      return options.fn(this);
    }
  }
  */
  if ((!(typeof obj == "object")) || (!Object.keys(obj).length)) {
    return options.inverse(this);
  }

  return options.fn(this);

});


Handlebars.registerHelper('ifShowFilter', function (aggregation, key, filters, options) {

  if (
    aggregation.buckets.length &&
    ! filters[ key ]
  ) {
    return options.fn(this);
  } else {
    return options.inverse(this);
  }

});

Handlebars.registerHelper('ifShowFilters', function (aggregations, filters, options) {

  if (Object.keys(filters).length) {
    for (filter in aggregations) {
      if (aggregations[filter].buckets.length && !filters[filter]) {
        return options.fn(this);
      }
    }
    return options.inverse(this);
  } else {
    return options.fn(this);
  }

});

Handlebars.registerHelper('nestedAggregation', function (aggregation) {
  return nestedAggregation(aggregation);
});

function nestedAggregation(aggregation, collapsed, id) {
  collapsed = typeof collapsed !== 'undefined' ? collapsed : false;
  id = typeof id !== 'undefined' ? id : false;

  var list = '<ul class="schema-tree collapse' + (collapsed ? '' : '.in') + '" ' + (id ? 'id="' + id + '"' : '') + '>';
  for (var key in aggregation) {
    if (typeof aggregation[key] == "object") {
      var class_id = key.split("/").slice(-1)[0];
      list +=
        '<li>' +
          '<div class="schema-tree-item">' +
            '<i class="fa fa-fw fa-tag schema-tree-icon"></i>' +
            '<a href="/resource/' + key + '">' +
              Packages.helpers.HandlebarsHelpers._i18n(key, null) + " (" + aggregation[key]["doc_count"] + ")" +
            '</a>' +
            (
              Object.keys(aggregation[key]).length > 1 ?
              '<a href="#' + class_id + '" class="schema-tree-plus collapsed" data-toggle="collapse">' +
                '<i class="fa fa-plus"></i>' +
                '<i class="fa fa-minus"></i>' +
              '</a>' :
              ''
            ) +
          '</div>' +
          nestedAggregation(aggregation[key], true, class_id) +
        '</li>';
    }
  }
  list += "</ul>";
  return Handlebars.SafeString(list);
}

// http://stackoverflow.com/a/16315366
Handlebars.registerHelper('ifCond', function (v1, operator, v2, options) {

  switch (operator) {
    case '==':
      return (v1 == v2) ? options.fn(this) : options.inverse(this);
    case '===':
      return (v1 === v2) ? options.fn(this) : options.inverse(this);
    case '<':
      return (v1 < v2) ? options.fn(this) : options.inverse(this);
    case '<=':
      return (v1 <= v2) ? options.fn(this) : options.inverse(this);
    case '>':
      return (v1 > v2) ? options.fn(this) : options.inverse(this);
    case '>=':
      return (v1 >= v2) ? options.fn(this) : options.inverse(this);
    case '&&':
      return (v1 && v2) ? options.fn(this) : options.inverse(this);
    case '||':
      return (v1 || v2) ? options.fn(this) : options.inverse(this);
    default:
      return options.inverse(this);
  }

});

Handlebars.registerHelper('ifIn', function(item, list, options) {
  for (i in list) {
    if (list[i] == item) {
      return options.fn(this);
    }
  }
  return options.inverse(this);
});
