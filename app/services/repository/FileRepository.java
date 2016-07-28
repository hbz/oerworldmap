package services.repository;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.jena.atlas.RuntimeIOException;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.indices.IndexMissingException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;

import helpers.JsonLdConstants;
import models.Resource;
import play.Logger;


public class FileRepository extends Repository implements Writable, Readable {

  private Client mClient;

  private TypeReference<HashMap<String, Object>> mMapType = new TypeReference<HashMap<String, Object>>() {
  };

  private Path getPath() {
    return Paths.get(mConfiguration.getString("filerepo.dir"));
  }

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
  public void addResource(@Nonnull final Resource aResource, Map<String, String> aMetadata) throws IOException {
    String id = aResource.getAsString(JsonLdConstants.ID);
    String encodedId = DigestUtils.sha256Hex(id);
    Path dir = Paths.get(getPath().toString(), aResource.getAsString(JsonLdConstants.TYPE));
    Path file = Paths.get(dir.toString(), encodedId);
    if (!Files.exists(dir)) {
      Files.createDirectory(dir);
    }
    Files.write(file, aResource.toString().getBytes());
  }

  public void refreshIndex(String aIndex) {
    try {
      mClient.admin().indices().refresh(new RefreshRequest(aIndex)).actionGet();
    } catch (IndexMissingException e) {
      Logger.error("Trying to refresh index \"" + aIndex + "\" in Elasticsearch.");
      e.printStackTrace();
    }
  }

  @Override
  public void addResources(@Nonnull List<Resource> aResources, Map<String, String> aMetadata) throws IOException {
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
    Path typeDir = Paths.get(getPath().toString(), aType);
    try (DirectoryStream<Path> resourceFiles = Files.newDirectoryStream(typeDir)) {
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
  public Resource deleteResource(@Nonnull String aId, Map<String, String> aMetadata) {
    Resource resource = this.getResource(aId);
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
