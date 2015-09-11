package services.repository;

import models.Resource;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * @author fo
 */
public interface Writable {

  /**
   * Add a new resource to the repository.
   * @param  aResource The resource to be added
   */
  void addResource(@Nonnull Resource aResource, @Nonnull String aType) throws IOException;

  /**
   *
   * @param  aId The ID of the resource to be deleted
   * @return The deleted resource
   */
  Resource deleteResource(@Nonnull String aId);

}
