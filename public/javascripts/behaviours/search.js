// --- search ---
Hijax.behaviours.search = {

  attach: function(context) {

    var form = $('form#search', context),
        input = form.find('input[name="q"]'),
        query = input.val(),
        regex = /([^ ]+:[^ ]+)/g;

    if (query) {
      var filters = query.match(regex);
      for (var i in filters) {
        var filter = $('<div class="query-filter"><span data-value="' + filters[i] + '">' + filters[i] + '</span></div>');
        filter.append($('<input type="submit" value="X" />').click(function() {
          $(this).closest('div.query-filter').find('span[data-value]').removeAttr('data-value');
          $(this).closest('div.query-filter').hide();
        }));
        form.append(filter);
      }
      input.val(query.replace(regex, '').trim());
    }

    form.submit(function() {
      var qstring = input.val();
      $(this).find('span[data-value]').each(function(){
        console.log($(this).attr('data-value'));
        qstring = qstring + ' ' + $(this).attr('data-value');
      });
      input.val(qstring);
    });

  }

}
