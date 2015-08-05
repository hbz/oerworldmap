package controllers;

import models.Record;
import models.Resource;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import play.Logger;
import play.mvc.Result;
import services.AggregationProvider;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author fo
 */
public class AggregationIndex extends OERWorldMap {

  public static Result list() throws IOException {
    @SuppressWarnings("rawtypes")

    Resource countryAggregation = mBaseRepository.query(AggregationProvider.getByCountryAggregation());

    Map<String,Object> scope = new HashMap<>();
    scope.put("countryAggregation", countryAggregation);
    return ok(render("Country Aggregations", "AggregationIndex/index.mustache", scope));

  }

}
