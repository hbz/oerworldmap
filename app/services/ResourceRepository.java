package services;

import java.util.List;

import javax.annotation.Nonnull;

import models.Resource;

public interface ResourceRepository {
  
  /**
   * Add a new resource to the repository.
   * @param aResource
   */
  public void addResource(Resource aResource);
  
  /**
   * Query all resources of a given type.
   * @param aType
   * @return all resources of a given type as a List.
   */
  public List<Resource> queryAll(@Nonnull String aType);
  
  /**
   * Get a Resource specified by the given identifier. 
   * @param aId
   * @return the Resource by the given identifier or null if no such Resource exists.
   */
  public Resource query(@Nonnull String aType, @Nonnull String aId);
  
}
