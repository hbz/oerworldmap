package services.repository;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nonnull;

import models.Resource;

/**
 * @author fo
 */
public interface Writable {

  /**
   * Add a new resource to the repository.
   *
   * @param aResource
   *          The resource to be added
   * @param aMetadata
   *          Map containing metadata such as author, timestamp etc
   */
  void addResource(@Nonnull Resource aResource, Map<String, String> aMetadata) throws IOException;

  /**
   *
   * @param aId
   *          The ID of the resource to be deleted
   * @return The deleted resource
   * @throws IOException
   */
  Resource deleteResource(@Nonnull String aId) throws IOException;

}
