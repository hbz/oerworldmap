package services.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import javax.annotation.Nonnull;

import org.json.simple.parser.ParseException;

import models.ResourceList;

/**
 * @author fo
 */
public interface Queryable {

  /**
   * Query for resources.
   * 
   * @param aQueryString
   *          A string describing the query
   * @param aFilters
   * @return A resource resembling the result set of resources matching the
   *         criteria given in the query string
   */
  ResourceList query(@Nonnull String aQueryString, int aFrom, int aSize, String aSortOrder,
      Map<String, ArrayList<String>> aFilters) throws IOException, ParseException;

}
