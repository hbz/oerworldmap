package services.repository;

import models.ModelCommon;
import org.elasticsearch.search.aggregations.AggregationBuilder;

import javax.annotation.Nonnull;
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
  ModelCommon aggregate(@Nonnull AggregationBuilder<?> aAggregationBuilder, final String... aIndices)
    throws IOException;

}
