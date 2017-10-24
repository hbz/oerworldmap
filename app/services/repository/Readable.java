package services.repository;

import models.Resource;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

/**
 * @author fo
 */
public interface Readable {

  /**
   * Get a Resource specified by the given identifier.
   * @param  aId The identifier of the resource
   * @return The resource
   */
  Resource getItem(@Nonnull String aId) throws IOException;

  List<Resource> getAll(@Nonnull String aType, final String... aIndices) throws IOException;

}
