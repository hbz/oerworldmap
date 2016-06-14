var Hijax = (function ($, Hijax) {

  var my = {

    attach: function(context) {

      $('[data-behaviour="chart-pie"]', context).each(function() {
        var json = JSON.parse( $(this).find('script[type="application/ld+json"]').html() );
        var data = {
          labels: json.buckets.map(function(bucket) {
            return bucket.key
          }),
          datasets: [
            {
              data: json.buckets.map(function(bucket) {
                return bucket.doc_count
              }),
              backgroundColor: json.buckets.map(function(bucket) {
                return '#'+(0x1000000+(Math.random())*0xffffff).toString(16).substr(1,6)
              })
            }
          ]
        };
        $(this).hide();
        var canvas = $('<canvas width="400" height="400"></canvas>');
        $(this).before(canvas);
        var chart = new Chart(canvas, {
            type: 'pie',
            data: data,
            options: {
              responsive: false
            }
        });
      });

    },


  }

  Hijax.behaviours.charts = my;
  return Hijax;

})(jQuery, Hijax);
