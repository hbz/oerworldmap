package controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import helpers.JsonLdConstants;
import models.Resource;
import models.ResourceList;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import play.mvc.Result;
import services.AggregationProvider;

/**
 * @author fo
 */
public class AggregationIndex extends OERWorldMap {

  public static Result list() throws IOException {

    Map<String, Object> scope = new HashMap<>();

    List<AggregationBuilder<?>> statisticsAggregations = new ArrayList<>();
    statisticsAggregations.add(AggregationProvider.getTypeAggregation(0));
    statisticsAggregations.add(AggregationProvider.getByCountryAggregation(5));
    statisticsAggregations.add(AggregationProvider.getServiceLanguageAggregation(5));
    statisticsAggregations.add(AggregationProvider.getServiceByTopLevelFieldOfEducationAggregation());
    statisticsAggregations.add(AggregationProvider.getServiceByGradeLevelAggregation(0));
    statisticsAggregations.add(AggregationProvider.getKeywordsAggregation(5));

    scope.put("statistics", mBaseRepository.aggregate(statisticsAggregations));
    scope.put("colors", Arrays.asList("#36648b", "#990000", "#ffc04c", "#3b7615", "#9c8dc7", "#bad1ad", "#663399",
      "#009380", "#627e45", "#6676b0", "#5ab18d"));

    return ok(render("Country Aggregations", "AggregationIndex/index.mustache", scope));

  }

}
