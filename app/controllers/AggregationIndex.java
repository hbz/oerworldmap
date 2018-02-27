package controllers;

import org.elasticsearch.search.aggregations.AggregationBuilder;
import play.Configuration;
import play.Environment;
import play.mvc.Result;
import services.QueryContext;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;

/**
 * @author fo
 */
public class AggregationIndex extends OERWorldMap {

  @Inject
  public AggregationIndex(Configuration aConf, Environment aEnv) {
    super(aConf, aEnv);
  }

  public Result list() throws IOException {

    Map<String, Object> scope = new HashMap<>();

    List<AggregationBuilder> statisticsAggregations = new ArrayList<>();
    /*statisticsAggregations.add(AggregationProvider.getTypeAggregation(1));
    statisticsAggregations.add(AggregationProvider.getByCountryAggregation(5));
    statisticsAggregations.add(AggregationProvider.getServiceLanguageAggregation(5));
    statisticsAggregations.add(AggregationProvider.getServiceByTopLevelFieldOfEducationAggregation());
    statisticsAggregations.add(AggregationProvider.getServiceByGradeLevelAggregation(1));
    statisticsAggregations.add(AggregationProvider.getKeywordsAggregation(5));
    statisticsAggregations.add(AggregationProvider.getLicenseAggregation(1));
    statisticsAggregations.add(AggregationProvider.getProjectByLocationAggregation(1));
    statisticsAggregations.add(AggregationProvider.getFunderAggregation(1));
    statisticsAggregations.add(AggregationProvider.getLikeAggregation(1));
    statisticsAggregations.add(AggregationProvider.getPrimarySectorsAggregation(1));
    statisticsAggregations.add(AggregationProvider.getSecondarySectorsAggregation(1));*/

    scope.put("statistics", mBaseRepository.aggregate(statisticsAggregations, new QueryContext(null)));
    scope.put("colors", Arrays.asList("#36648b", "#990000", "#ffc04c", "#3b7615", "#9c8dc7", "#bad1ad", "#663399",
      "#009380", "#627e45", "#6676b0", "#5ab18d"));

    return ok(render("Country Aggregations", "AggregationIndex/index.mustache", scope));

  }

}
