package services;

import helpers.JsonLdConstants;
import models.Resource;
import services.repository.Readable;
import services.repository.Repository;
import services.repository.Writable;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fo
 */
public class MockResourceRepository extends Repository implements Readable, Writable {

  private Map<String, Resource> db = new HashMap<>();

  public MockResourceRepository() {
    super(null);
  }

  public void addResource(@Nonnull Resource aResource) throws IOException {
    addResource(aResource, new HashMap<>());
  }

  /**
   * Add a new resource to the repository.
   *
   * @param aResource
   * @param aMetadata
   */
  @Override
  public void addResource(@Nonnull Resource aResource, Map<String, String> aMetadata) throws IOException {
    String id = aResource.getAsString(JsonLdConstants.ID);
    db.put(id, aResource);
  }

  /**
   * Add a new resource to the repository.
   *
   * @param aResources
   * @param aMetadata
   */
  @Override
  public void addResources(@Nonnull List<Resource> aResources, Map<String, String> aMetadata) throws IOException {
    for (Resource resource : aResources) {
      addResource(resource, aMetadata);
    }
  }

  /**
   * Get a Resource specified by the given identifier.
   *
   * @param aId
   * @return the Resource
   */
  @Override
  public Resource getResource(@Nonnull String aId) throws IOException {
    return db.get(aId);
  }

  @Override
  public Resource deleteResource(@Nonnull String aId, Map<String, String> aMetadata) {
    Resource resource = db.get(aId);
    db.remove(aId);
    return resource;
  }

  @Override
  public List<Resource> getAll(final String[] aIndices, @Nonnull String aType) {
    return new ArrayList<>(db.values());
  }

  public int size() {
    return db.size();
  }

  public String toString() {
    return db.toString();
  }

}
