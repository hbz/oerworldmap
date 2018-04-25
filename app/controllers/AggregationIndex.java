package controllers;

import models.Resource;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import play.Configuration;
import play.Environment;
import play.mvc.Result;
import services.AggregationProvider;
import services.QueryContext;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
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

    Map<String, Object> scope = new HashMap<>();

    List<AggregationBuilder> statisticsAggregations = new ArrayList<>();
    statisticsAggregations.add(AggregationProvider.getTypeAggregation(1));
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
    statisticsAggregations.add(AggregationProvider.getSecondarySectorsAggregation(1));

    // Enrich with aggregation labels
    Resource aggregations = mBaseRepository.aggregate(statisticsAggregations, new QueryContext(null));
    for (String agg : aggregations.keySet()) {
      if (agg.endsWith("@id")) {
        for (Resource bucket : aggregations.getAsResource(agg).getAsList("buckets")) {
          bucket.put("label", mBaseRepository.getResource(bucket.getAsString("key")).getAsList("name"));
        }
      }
    }

    return ok(aggregations.toJson());
  }

}
