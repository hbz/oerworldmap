package services.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import helpers.JsonLdConstants;
import models.ModelCommon;
import models.Resource;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.jena.atlas.RuntimeIOException;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class FileRepository extends Repository implements Writable, Readable {

  private TypeReference<HashMap<String, Object>> mMapType =
    new TypeReference<HashMap<String, Object>>() {};
  private Path getPath() {
    return Paths.get(mConfiguration.getString("filerepo.dir"));
  }
  private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public FileRepository(Config aConfiguration) {
    super(aConfiguration);
  }

  /**
   * Add a new resource to the repository.
   *
   * @param aResource
   * @param aMetadata
   */
  @Override
  public void addItem(@Nonnull final ModelCommon aResource, Map<String, Object> aMetadata) throws IOException {
    String id = aResource.getAsString(JsonLdConstants.ID);
    String encodedId = DigestUtils.sha256Hex(id);
    Path dir = Paths.get(getPath().toString(), aResource.getAsString(JsonLdConstants.TYPE));
    Path file = Paths.get(dir.toString(), encodedId);
    if (!Files.exists(dir)) {
      Files.createDirectory(dir);
    }
    Files.write(file, aResource.toString().getBytes());
  }

  @Override
  public void addItems(@Nonnull List<ModelCommon> aResources, Map<String, Object> aMetadata) throws IOException {
    throw new UnsupportedOperationException();
  }

  /**
   * Get a Resource specified by the given identifier.
   *
   * @param aId
   * @return the Resource by the given identifier or null if no such Resource
   * exists.
   */
  @Override
  public Resource getItem(@Nonnull String aId) {
    Path resourceFile;
    try {
      resourceFile = getResourcePath(aId);
      Map<String, Object> resourceMap = OBJECT_MAPPER.readValue(resourceFile.toFile(), mMapType);
      return new Resource(resourceMap);
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
  public List<ModelCommon> getAll(@Nonnull String aType, String... aIndices) {
    ArrayList<ModelCommon> results = new ArrayList<>();
    Path typeDir = Paths.get(getPath().toString(), aType);
    try (DirectoryStream<Path> resourceFiles = Files.newDirectoryStream(typeDir)) {
      for (Path resourceFile : resourceFiles) {
        Map<String, Object> resourceMap;
        try {
          resourceMap = OBJECT_MAPPER.readValue(resourceFile.toFile(), mMapType);
        } catch (IOException ex) {
          ex.printStackTrace();
          continue;
        }
        results.add(new Resource(resourceMap));
      }
    } catch (IOException e) {
      throw new RuntimeIOException(e);
    }
    return results;
  }


  /**
   * Delete a Resource specified by the given identifier.
   *
   * @param aId
   * @param aMetadata
   * @return The resource that has been deleted.
   */
  @Override
  public ModelCommon deleteResource(@Nonnull String aId, @Nonnull String aClassType, Map<String, Object> aMetadata) {
    Resource resource = this.getItem(aId);
    try {
      Files.delete(getResourcePath(aId));
    } catch (IOException e) {
      return null;
    }
    return resource;
  }

  private Path getResourcePath(@Nonnull final String aId) throws IOException {
    String encodedId = DigestUtils.sha256Hex(aId);
    DirectoryStream.Filter<Path> directoryFilter = new DirectoryStream.Filter<Path>() {
      @Override
      public boolean accept(Path entry) throws IOException {
        return Files.isDirectory(entry);
      }
    };

    try (DirectoryStream<Path> typeDirs = Files.newDirectoryStream(getPath(),directoryFilter)) {
      for (Path typeDir : typeDirs) {
        DirectoryStream.Filter<Path> fileFilter =  new DirectoryStream.Filter<Path>() {
          @Override
          public boolean accept(Path entry) throws IOException {
            return (entry.getFileName().toString().equals(encodedId));
          }
        };
        try (DirectoryStream<Path> resourceFiles = Files.newDirectoryStream(typeDir, fileFilter)) {
          Iterator<Path> iterator = resourceFiles.iterator();
          if (iterator.hasNext()) {
            return iterator.next();
          }
        }
      }
    }

    throw new IOException(aId + " not found.");

  }

}
