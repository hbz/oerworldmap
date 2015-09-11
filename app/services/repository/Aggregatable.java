package services.repository;

import models.Resource;
import org.elasticsearch.search.aggregations.AggregationBuilder;

import java.io.IOException;

/**
 * @author fo
 */
public interface Aggregatable {

  /**
   * Get an aggregation
   * @param  aAggregationBuilder The builder describing the aggregation
   * @return A resource resembling the reqeusted aggregation
   * @throws IOException
   */
  Resource aggregate(AggregationBuilder<?> aAggregationBuilder) throws IOException;

}
