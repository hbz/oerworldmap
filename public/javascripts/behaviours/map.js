var heat_data_dump = {"@type":"Aggregation","@id":"country-list","entries":[{"value":37,"key":"US"},{"value":34,"key":"DE"},{"value":21,"key":"GB"},{"value":16,"key":"CA"},{"value":15,"key":"ES"},{"value":13,"key":"RU"},{"value":11,"key":"AU"},{"value":8,"key":"ZA"},{"value":7,"key":"BR"},{"value":6,"key":"IN"},{"value":6,"key":"IT"},{"value":4,"key":"BE"},{"value":4,"key":"MX"},{"value":4,"key":"PL"},{"value":4,"key":"UA"},{"value":3,"key":"CH"},{"value":3,"key":"EC"},{"value":3,"key":"FR"},{"value":3,"key":"GR"},{"value":3,"key":"IE"},{"value":3,"key":"JP"},{"value":3,"key":"NL"},{"value":3,"key":"NZ"},{"value":3,"key":"PT"},{"value":3,"key":"RO"},{"value":2,"key":"AL"},{"value":2,"key":"HR"},{"value":2,"key":"SE"},{"value":1,"key":"AM"},{"value":1,"key":"AT"},{"value":1,"key":"AW"},{"value":1,"key":"BH"},{"value":1,"key":"CL"},{"value":1,"key":"CN"},{"value":1,"key":"CO"},{"value":1,"key":"CZ"},{"value":1,"key":"DM"},{"value":1,"key":"EE"},{"value":1,"key":"FI"},{"value":1,"key":"FJ"},{"value":1,"key":"GE"},{"value":1,"key":"HU"},{"value":1,"key":"ID"},{"value":1,"key":"IR"},{"value":1,"key":"IS"},{"value":1,"key":"KE"},{"value":1,"key":"KR"},{"value":1,"key":"LB"},{"value":1,"key":"LT"},{"value":1,"key":"MY"},{"value":1,"key":"NA"},{"value":1,"key":"OM"},{"value":1,"key":"PA"},{"value":1,"key":"RS"},{"value":1,"key":"SI"},{"value":1,"key":"SN"},{"value":1,"key":"TG"},{"value":1,"key":"TH"},{"value":1,"key":"TN"},{"value":1,"key":"TR"},{"value":1,"key":"TT"},{"value":1,"key":"UY"},{"value":1,"key":"VE"}]};

Hijax.behaviours.map = {
  
  heat_data : false,
  color : false,
  
  throttledTimer : false,
  
  width : false,
  height : false,
  
  zoom : false,
  
  topo : false,
  projection : false,
  path : false,
  svg : false,
  g : false,
  
  attach : function(context) {
    var that = this;
    
    that.context = context;
    
    // return if no map in context
    if(! $('div[data-view="map"]', that.context).length) {
      return;
    }
    
    // create map container
    that.container = $('<div id="map"></div>')[0];
    
    // append to view
    $('div[data-view="map"]').prepend(that.container);
    
    // hide table
    $( that.container ).siblings("table").hide();
    
    // set mapview
    $( that.container ).closest('div[role="main"], div[role="complementary"]').addClass("map-view");
    
    that.getHeatData();
    
    var heats = $.map(that.heat_data, function(value, index){
      return [value];
    });
    //heats = $.makeArray(map.heat_data);
    console.log(heats);
    
    that.color = d3.scale.log()
      .range(["#a1cd3f", "#eaf0e2"])
      .interpolate(d3.interpolateHcl)
      .domain([d3.quantile(heats, .01), d3.quantile(heats, .99)]);
      
    that.zoom = d3.behavior.zoom()
      .scaleExtent([1, 20])
      .on("zoom", that.move);
    that.setup();
    that.loadMapData();
    
    //console.log( that.zoom );
    
/*
    $.get(
      'https://oerworldmap.org',
      function(data){
        console.log(
          $('#users-by-country script', data).html()
        );
      }
    )
*/
  },
  
  getHeatData : function() { //console.log("getHeatData");
    var that = this;
    
    that.heat_data = {};
    for(var i = 0; i < heat_data_dump["entries"].length; i++) {
      //console.log(heat_data_dump["entries"][i]);
      that.heat_data[ heat_data_dump["entries"][i].key ] = heat_data_dump["entries"][i].value;
    }
  },
  
  onResize : function() {
    var that = this;
    
    that.doThrottled(function(){
      d3.select('svg').remove();
      that.setup();
      that.draw(that.topo);
    });
  },
  
  move : function() {
    var that = Hijax.behaviours.map; //console.log(Hijax.behaviours.map.zoom);
    
    var t = d3.event.translate;
    var s = d3.event.scale;
    var h = that.height / 4;
    
    t[0] = Math.min(
      (that.width / that.height) * (s - 1), 
      Math.max(that.width * (1 - s), t[0])
    );

    t[1] = Math.min(
      h * (s - 1) + h * s, 
      Math.max(that.height	 * (1 - s) - h * s, t[1])
    );

    that.zoom.translate(t);
    that.g.attr("transform", "translate(" + t + ")scale(" + s + ")");
  
    //adjust the country stroke width based on zoom level
    that.doThrottled(function(){
      d3.selectAll(".country").style("stroke-width", 0.5 / s);
    });
  },

  doThrottled: function(callback) {
    var that = this;
    
    window.clearTimeout( that.throttledTimer );
    that.throttledTimer = window.setTimeout(function(){
      callback();
    }, 200);
  },
  
  loadMapData : function() {
    var that = this;
    
    //d3.json("/assets/playground/map1/world-topo-min.json", function(error, world) {
    d3.json("/assets/json/ne_50m_admin_0_countries_topo.json", function(error, world) {
      that.topo = topojson.feature(world, world.objects.ne_50m_admin_0_countries).features;
      that.draw( that.topo );
    });
  },
  
  setup : function() {
    var that = this;
    
    that.width = that.container.offsetWidth;
    that.height = that.container.offsetHeight;
    
    console.log(that.width, that.height);
    
    that.projection = d3.geo.miller()
      .translate([
        (that.width / 2),
        (that.height / 2)
      ]).scale( that.width / 2 / Math.PI );
    
    that.path = d3.geo.path().projection(that.projection);
    
    that.svg = d3
      .select( "#map" ).append("svg")
      .attr("width", that.width)
      .attr("height", that.height)
      .call( that.zoom )
      .on("click", that.click);
  
    that.g = that.svg.append("g");
  },
  
  draw : function() {
    var that = this;
  
    var country = that.g.selectAll(".country").data( that.topo );
  
    country.enter().insert("path")
      .attr("class", "country")
      .attr("d", that.path)
      .attr("id", function(d,i) { return d.id; })
      .attr("title", function(d,i) { return d.properties.name; })
      .style("fill", function(d,i) {
        if(that.heat_data[ d.id ]) {
          return that.color( that.heat_data[ d.id ] || 1 );
        } else {
          return "#ffffff";
        }
      });
      
    country.on("click", function(){ console.log("country clicked");
      d3.select(this).style("fill", "#aaa");
    })
  
    // add some capitals from external CSV file
/*
    d3.csv("/assets/playground/map1/country-capitals.csv", function(err, capitals) {
      capitals.forEach(function(i){
        that.addPoint(i.CapitalLongitude, i.CapitalLatitude, i.CapitalName );
      });
    });
*/
  
  },
  
  getHeatColor(val) {
    return "hsl()"
  },
  
  addPoint : function(lat, lon, text) {
    var that = this;
  
    var gpoint = that.g.append("g").attr("class", "gpoint");
    var x = that.projection([lat,lon])[0];
    var y = that.projection([lat,lon])[1];
  
    gpoint.append("svg:circle")
      .attr("cx", x)
      .attr("cy", y)
      .attr("class","point")
      .attr("r", 1.5);
    
    if(text.length > 0) {	
      gpoint.append("text")
        .attr("x", x + 3)
        .attr("y", y + 3)
        .attr("class", "text")
        .text(text);
    }

  }

};