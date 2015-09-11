package services.repository;

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

public class FileRepository implements Writable, Readable {

  /**
   * The file system path where resources are stored
   */
  private Path mPath;

  private TypeReference<HashMap<String, Object>> mMapType = new TypeReference<HashMap<String, Object>>() {
  };

  /**
   * Construct FileRepository.
   * 
   * @param aPath The file system path where resources are stored
   */
  public FileRepository(Path aPath) throws IOException {
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
   * Query all resources of a given type.
   *
   * @param aType The type of the resources to get
   * @return All resources of the given type as a List.
   */
  @Override
  public List<Resource> getAll(@Nonnull String aType) {
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
