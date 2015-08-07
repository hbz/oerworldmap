package services;

import com.fasterxml.jackson.core.type.TypeReference;
import helpers.JsonLdConstants;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import javax.annotation.Nonnull;

import models.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

public class FileResourceRepository implements ResourceRepository {

  /**
   * The file system path where resources are stored
   */
  private Path mPath;

  private TypeReference<HashMap<String, Object>> mMapType = new TypeReference<HashMap<String, Object>>() {
  };

  /**
   * Construct FileResourceRepository.
   * 
   * @param aPath The file system path where resources are stored
   */
  public FileResourceRepository(Path aPath) throws IOException {
    if (aPath == null || !Files.exists(aPath)) {
      throw new IOException(aPath + " not existing.");
    }
    if (!Files.isWritable(aPath)) {
      throw new IOException(aPath + " not writable.");
    }
    mPath = aPath;
  }

  /**
   * Add a new resource to the repository.
   * 
   * @param aResource
   */
  @Override
  public void addResource(@Nonnull Resource aResource) throws IOException {
    String type = (String) aResource.get(JsonLdConstants.TYPE);
    addResource(aResource, type);
  }

  /**
   * Add a new resource to the repository.
   *
   * @param aResource
   */
  public void addResource(@Nonnull Resource aResource, @Nonnull String aType) throws IOException {
    String id = (String) aResource.get(JsonLdConstants.ID);
    Path dir = Paths.get(mPath.toString(), aType);
    Path file = Paths.get(dir.toString(), id);
    if (!Files.exists(dir)) {
      Files.createDirectory(dir);
    }
    Files.write(file, aResource.toString().getBytes());
  }

  /**
   * Get a Resource specified by the given identifier.
   * 
   * @param aId
   * @return the Resource by the given identifier or null if no such Resource
   * exists.
   */
  @Override
  public Resource getResource(@Nonnull String aId) {
    ObjectMapper objectMapper = new ObjectMapper();
    Path resourceFile;
    try {
      resourceFile = getResourcePath(aId);
      Map<String, Object> resourceMap = objectMapper.readValue(resourceFile.toFile(), mMapType);
      return Resource.fromMap(resourceMap);
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * Delete a Resource specified by the given identifier.
   * 
   * @param aId
   * @return The resource that has been deleted.
   */
  public Resource deleteResource(@Nonnull String aId) {
    Resource resource = this.getResource(aId);
    try {
      Files.delete(getResourcePath(aId));
    } catch (IOException e) {
      return null;
    }
    return resource;
  }

  /**
   * Query all resources of a given type.
   * 
   * @param aType
   * @return all resources of a given type as a List.
   */
  @Override
  public List<Resource> query(@Nonnull String aType) {
    ArrayList<Resource> results = new ArrayList<>();
    Path typeDir = Paths.get(mPath.toString(), aType);
    DirectoryStream<Path> resourceFiles;
    try {
      resourceFiles = Files.newDirectoryStream(typeDir);
    } catch (IOException ex) {
      ex.printStackTrace();
      return results;
    }
    ObjectMapper objectMapper = new ObjectMapper();
    for (Path resourceFile : resourceFiles) {
      Map<String, Object> resourceMap;
      try {
        resourceMap = objectMapper.readValue(resourceFile.toFile(), mMapType);
      } catch (IOException ex) {
        ex.printStackTrace();
        continue;
      }
      results.add(Resource.fromMap(resourceMap));
    }
    return results;
  }

  /**
   * Get a (Linked) List of Resources that are of the specified type and have
   * the specified content in that specified field.
   *
   * @param aType
   * @param aField
   * @param aContent
   * @return all matching Resources or an empty list if no resources match the
   * given field / content combination.
   */
  public List<Resource> getResourcesByContent(@Nonnull String aType, @Nonnull String aField,
      String aContent) {
    if (StringUtils.isEmpty(aType) || StringUtils.isEmpty(aField)) {
      throw new IllegalArgumentException("Non-complete arguments.");
    } else {
      List<Resource> result = new LinkedList<>();
      List<Resource> resources = query(aType);
      for (Resource resource : resources) {
        if (null != resource.get(aField) && resource.get(aField).toString().equals(aContent)) {
          result.add(resource);
        }
      }
      return result;
    }
  }

  private Path getResourcePath(@Nonnull final String aId) throws IOException {

    DirectoryStream<Path> typeDirs = Files.newDirectoryStream(mPath,
        new DirectoryStream.Filter<Path>() {
          @Override
          public boolean accept(Path entry) throws IOException {
            return Files.isDirectory(entry);
          }
        });

    for (Path typeDir : typeDirs) {
      DirectoryStream<Path> resourceFiles = Files.newDirectoryStream(typeDir,
          new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
              return (entry.getFileName().toString().equals(aId));
            }
          });
      for (Path resourceFile : resourceFiles) {
        return resourceFile;
      }
    }

    throw new IOException(aId + " not found.");

  }

}
