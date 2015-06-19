package services;

import java.util.List;

import java.io.IOException;
import javax.annotation.Nonnull;

import models.Resource;

public interface ResourceRepository {

  /**
   * Add a new resource to the repository.
   * @param aResource
   */
  public void addResource(@Nonnull Resource aResource) throws IOException;

  /**
   * Get a Resource specified by the given identifier.
   * @param aId
   * @return the Resource
   */
  public Resource getResource(@Nonnull String aId) throws IOException;

  /**
   * Query all resources of a given type.
   * @param aType
   * @return the Resource by the given identifier or null if no such Resource exists.
   */
  public List<Resource> query(@Nonnull String aType) throws IOException;

  public List<Resource> getResourcesByContent(@Nonnull String aType, @Nonnull String aField, String aContent);

  }
