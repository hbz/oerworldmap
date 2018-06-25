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

    List<AggregationBuilder> statisticsAggregations = new ArrayList<>();
    statisticsAggregations.add(AggregationProvider.getTypeAggregation(0));
    statisticsAggregations.add(AggregationProvider.getByCountryAggregation(5));
    statisticsAggregations.add(AggregationProvider.getServiceLanguageAggregation(5));
    statisticsAggregations.add(AggregationProvider.getServiceByTopLevelFieldOfEducationAggregation());
    statisticsAggregations.add(AggregationProvider.getServiceByGradeLevelAggregation(0));
    statisticsAggregations.add(AggregationProvider.getKeywordsAggregation(5));
    statisticsAggregations.add(AggregationProvider.getLicenseAggregation(0));
    statisticsAggregations.add(AggregationProvider.getProjectByLocationAggregation(0));
    statisticsAggregations.add(AggregationProvider.getFunderAggregation(0));
    statisticsAggregations.add(AggregationProvider.getLikeAggregation(0));
    statisticsAggregations.add(AggregationProvider.getPrimarySectorsAggregation(0));
    statisticsAggregations.add(AggregationProvider.getSecondarySectorsAggregation(0));

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
