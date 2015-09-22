package services;

import com.typesafe.config.Config;
import helpers.JsonLdConstants;
import models.Resource;
import services.repository.*;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fo
 */
public class MockResourceRepository extends Repository implements services.repository.Readable, Writable {

  private Map<String, Resource> db = new HashMap<>();

  public MockResourceRepository() {
    super(null);
  }

  public void addResource(@Nonnull Resource aResource) throws IOException {
    addResource(aResource, "Thing");
  }

  /**
   * Add a new resource to the repository.
   * 
   * @param aResource
   */
  public void addResource(@Nonnull Resource aResource, @Nonnull String aType) throws IOException {
    String id = aResource.getAsString(JsonLdConstants.ID);
    db.put(id, aResource);
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
  public Resource deleteResource(@Nonnull String aId) {
    Resource resource = db.get(aId);
    db.remove(aId);
    return resource;
  }

  @Override
  public List<Resource> getAll(@Nonnull String aType) {
    return new ArrayList<>(db.values());
  }
  
  public int size(){
    return db.size();
  }

}