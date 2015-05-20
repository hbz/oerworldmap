package controllers;

import models.Record;
import models.Resource;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import play.mvc.Result;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author fo
 */
public class AggregationIndex extends OERWorldMap {

  public static Result list() throws IOException {
    @SuppressWarnings("rawtypes")
    AggregationBuilder aggregationBuilder = AggregationBuilders.terms("by_country").field(
        Record.RESOURCEKEY + ".workLocation.address.addressCountry").size(0);
    Resource countryAggregation = mBaseRepository.query(aggregationBuilder);
    Map<String,Object> scope = new HashMap<>();
    scope.put("countryAggregation", countryAggregation);
    return ok(render("Home", "AggregationIndex/index.mustache", scope));
  }

}
