package services;

import helpers.JsonLdConstants;
import models.Resource;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fo
 */
public class MockResourceRepository implements ResourceRepository {

  private Map<String, Resource> db = new HashMap<>();

  /**
   * Add a new resource to the repository.
   * 
   * @param aResource
   */
  public void addResource(@Nonnull Resource aResource) throws IOException {
    String id = aResource.get(JsonLdConstants.ID).toString();
    db.put(id, aResource);
  }

  /**
   * Get a Resource specified by the given identifier.
   * 
   * @param aId
   * @return the Resource
   */
  public Resource getResource(@Nonnull String aId) throws IOException {
    return db.get(aId);
  }

  public Resource deleteResource(@Nonnull String aId) {
    Resource resource = db.get(aId);
    db.remove(aId);
    return resource;
  }

  /**
   * Query all resources of a given type.
   * 
   * @param aType
   * @return the Resource by the given identifier or null if no such Resource
   *         exists.
   */
  public List<Resource> query(@Nonnull String aType) throws IOException {
    return new ArrayList<>();
  }

  public List<Resource> getResourcesByContent(@Nonnull String aType, @Nonnull String aField,
      String aContent) {
    return new ArrayList<>();
  }

}