package services.repository;

import models.ModelCommon;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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
  void addItem(@Nonnull ModelCommon aResource, Map<String, Object> aMetadata) throws IOException;

  /**
   * Add multiple resources to the repository
   *
   * @param aResources
   *          The resources to be added
   * @param aMetadata
   *          Map containing metadata such as author, timestamp etc
   * @throws IOException
   */
  void addItems(@Nonnull List<ModelCommon> aResources, Map<String, Object> aMetadata) throws IOException;

  /**
   * Delete a resource from the repository
   *
   * @param aId
   *          The ID of the resource to be deleted
   * @param aClazz
   *          The class of the resource to be deleted
   * @param aMetadata
   *          Map containing metadata such as author, timestamp etc
   * @return The deleted resource
   * @throws IOException
   */
  ModelCommon deleteItem(@Nonnull final String aId,
                         @Nonnull final Class aClazz,
                         final Map<String, Object> aMetadata) throws IOException;

}
