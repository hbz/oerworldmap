package controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.search.aggregations.AggregationBuilder;
import play.mvc.Result;
import services.AggregationProvider;
import services.QueryContext;

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
    statisticsAggregations.add(AggregationProvider.getLicenseAggregation(0));

    scope.put("statistics", mBaseRepository.aggregate(statisticsAggregations, new QueryContext(null)));
    scope.put("colors", Arrays.asList("#36648b", "#990000", "#ffc04c", "#3b7615", "#9c8dc7", "#bad1ad", "#663399",
      "#009380", "#627e45", "#6676b0", "#5ab18d"));

    return ok(render("Country Aggregations", "AggregationIndex/index.mustache", scope));

  }

}
