package services.repository;

import models.ModelCommonList;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author fo
 */
public interface Queryable {

  /**
   * Query for resources.
   *
   * @param aQueryString A string describing the query.
   * @param aFrom Pointer to the first resource to be returned from a list of multiple matches.
   * @param aSize Number of resources to be returned.
   * @param aSortOrder Define the list's criterium for sorting.
   * @param aFilters
   * @param aIndices
   * @return A resource resembling the result set of resources matching the
   *         criteria given in the query string
   */
  ModelCommonList query(@Nonnull String aQueryString, int aFrom, int aSize, String aSortOrder,
                        Map<String, List<String>> aFilters, final String... aIndices) throws IOException;

}
