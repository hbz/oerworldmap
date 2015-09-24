package controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import models.Resource;
import play.mvc.Result;
import services.AggregationProvider;

/**
 * @author fo
 */
public class AggregationIndex extends OERWorldMap {

  public static Result list() throws IOException {

    Resource countryAggregation = mBaseRepository
        .aggregate(AggregationProvider.getByCountryAggregation());
    Map<String, Object> scope = new HashMap<>();
    scope.put("countryAggregation", countryAggregation);
    return ok(render("Country Aggregations", "AggregationIndex/index.mustache", scope));

  }

}
