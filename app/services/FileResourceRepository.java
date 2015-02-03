package services;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Iterator;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.io.IOException;
import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;

import helpers.JsonLdConstants;
import models.Resource;

public class FileResourceRepository implements ResourceRepository {

  /**
   * The file system path where resources are stored
   */
  private Path mPath;

  /**
   * Construct FileResourceRepository.
   * @param aPath The file system path where resources are stored
   */
  public FileResourceRepository(Path aPath) throws IOException {
    if (!Files.exists(aPath) || !Files.isWritable(aPath)) {
      throw new IOException(aPath + " not writable.");
    }
    mPath = aPath;
  }

  /**
   * Add a new resource to the repository.
   * @param aResource
   */
  @Override
  public void addResource(Resource aResource) throws IOException {
    String id = (String)aResource.get(JsonLdConstants.ID);
    String type = (String)aResource.get(JsonLdConstants.TYPE);
    Path dir = Paths.get(mPath.toString(), type);
    Path file = Paths.get(dir.toString(), id);
    if (!Files.exists(dir)) {
      Files.createDirectory(dir);
    }
    Files.write(file, aResource.toString().getBytes());
  }

  /**
   * Get a Resource specified by the given identifier.
   * @param aId
   * @return the Resource by the given identifier or null if no such Resource exists.
   */
  @Override
  public Resource getResource(@Nonnull String aId) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    Path resourceFile = getResourcePath(aId);
    return Resource.fromMap(objectMapper.readValue(resourceFile.toFile(), Map.class));
  }
  
  /**
   * Delete a Resource specified by the given identifier.
   * @param aId
   * @return The resource that has been deleted.
   */
  public Resource deleteResource(@Nonnull String aId) throws IOException {
    Resource resource = this.getResource(aId);
    Files.delete(getResourcePath(aId));
    return resource;
  }

  /**
   * Query all resources of a given type.
   * @param aType
   * @return all resources of a given type as a List.
   */
  @Override
  public List<Resource> query(@Nonnull String aType) throws IOException {
    ArrayList<Resource> results = new ArrayList<Resource>();
    Path typeDir = Paths.get(mPath.toString(), aType);
    DirectoryStream<Path> resourceFiles = Files.newDirectoryStream(typeDir);
    ObjectMapper objectMapper = new ObjectMapper();
    for (Path resourceFile: resourceFiles) {
      results.add(Resource.fromMap(objectMapper.readValue(resourceFile.toFile(), Map.class)));
    }
    return results;
  }
  
  private Path getResourcePath(@Nonnull String aId) throws IOException {
    
    DirectoryStream<Path> typeDirs = Files.newDirectoryStream(mPath,
            new DirectoryStream.Filter<Path>() {
              @Override
              public boolean accept(Path entry) throws IOException
              {
                return Files.isDirectory(entry);
              }
            }
    );

    for (Path typeDir: typeDirs) {
      DirectoryStream<Path> resourceFiles = Files.newDirectoryStream(typeDir,
              new DirectoryStream.Filter<Path>() {
                @Override
                public boolean accept(Path entry) throws IOException
                {
                  return (entry.getFileName().toString().equals(aId));
                }
              }
      );
      for (Path resourceFile: resourceFiles) {
        return resourceFile;
      }
    }

    throw new IOException(aId + " not found.");
    
  }

}
