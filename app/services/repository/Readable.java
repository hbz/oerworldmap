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
  Resource getResource(@Nonnull String aId) throws IOException;

  List<Resource> getAll(final String[] aIndices, @Nonnull String aType) throws IOException;

}
