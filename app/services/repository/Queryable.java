package services.repository;

import models.ResourceList;
import org.json.simple.parser.ParseException;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * @author fo
 */
public interface Queryable {

  /**
   * Query for resources.
   * @param  aQueryString A string describing the query
   * @return A resource resembling the result set of resources matching the criteria given in the query string
   */
  ResourceList query(@Nonnull String aQueryString, int aFrom, int aSize, String aSortOrder) throws IOException, ParseException;

}
