package controllers;

import org.elasticsearch.search.aggregations.AggregationBuilder;
import play.Configuration;
import play.Environment;
import play.mvc.Result;
import services.AggregationProvider;
import services.QueryContext;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fo
 */
public class AggregationIndex extends OERWorldMap {

  @Inject
  public AggregationIndex(Configuration aConf, Environment aEnv) {
    super(aConf, aEnv);
  }

  public Result list() throws IOException {

    List<AggregationBuilder<?>> statisticsAggregations = new ArrayList<>();
    statisticsAggregations.add(AggregationProvider.getTypeAggregation(0));
    statisticsAggregations.add(AggregationProvider.getByCountryAggregation(5));
    statisticsAggregations.add(AggregationProvider.getServiceLanguageAggregation(5));
    statisticsAggregations.add(AggregationProvider.getServiceByTopLevelFieldOfEducationAggregation());
    statisticsAggregations.add(AggregationProvider.getServiceByGradeLevelAggregation(0));
    statisticsAggregations.add(AggregationProvider.getKeywordsAggregation(5));
    statisticsAggregations.add(AggregationProvider.getLicenseAggregation(0));
    statisticsAggregations.add(AggregationProvider.getProjectByLocationAggregation(0));
    statisticsAggregations.add(AggregationProvider.getFunderAggregation(0));
    statisticsAggregations.add(AggregationProvider.getPrimarySectorsAggregation(0));
    statisticsAggregations.add(AggregationProvider.getSecondarySectorsAggregation(0));

    return ok(mBaseRepository.aggregate(statisticsAggregations, new QueryContext(null)).toJson());

  }

}
