package services.repository;

import models.ModelCommon;

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
  ModelCommon getItem(@Nonnull String aId) throws IOException;

  List<ModelCommon> getAll(@Nonnull String aType, final String... aIndices) throws IOException;

}
