package helpers;

import models.Resource;
import models.ResourceList;
import org.apache.commons.io.IOUtils;
import play.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface JsonTest {

  default Resource getResourceFromJsonFile(String aFile) throws IOException {
    InputStream in = ClassLoader.getSystemResourceAsStream(aFile);
    String json = IOUtils.toString(in, StandardCharsets.UTF_8);
    return Resource.fromJson(json);
  }

  default Resource getResourceFromJsonFileUnsafe(String aFile) {
    InputStream in = ClassLoader.getSystemResourceAsStream(aFile);
    try {
      String json = IOUtils.toString(in, StandardCharsets.UTF_8);
      return Resource.fromJson(json);
    } catch (IOException e) {
      Logger.error(e.toString());
      return new Resource(null);
    }
  }

  default ResourceList getResourcesFromPagedCollectionFile(String aPagedCollectionFile)
    throws IOException {
    InputStream in = ClassLoader.getSystemResourceAsStream(aPagedCollectionFile);
    String json = IOUtils.toString(in, StandardCharsets.UTF_8);
    return new ResourceList(Resource.fromJson(json));
  }

  default List<Resource> getResourcesFromJsonDir(String aDir) throws IOException {
    List<Resource> resources = new ArrayList<>();
    try {
      URL pathURL = ClassLoader.getSystemResource(aDir);
      if ((pathURL != null) && pathURL.getProtocol().equals("file")) {
        for (String file : new File(pathURL.toURI()).list()) {
          if (file.endsWith(".json")) {
            InputStream in = ClassLoader.getSystemResourceAsStream(aDir.concat(file));
            String json = IOUtils.toString(in, StandardCharsets.UTF_8);
            resources.add(Resource.fromJson(json));
          }
        }
      }
    } catch (URISyntaxException e) {
      Logger.error(e.toString());
    }
    return resources;
  }

  default String getStringFromFile(String aPath, Charset aEncoding)
    throws IOException {
    byte[] encoded = Files
      .readAllBytes(Paths.get(ClassLoader.getSystemResource(aPath).toExternalForm().substring(5)));
    return new String(encoded, aEncoding);
  }

  default List<String> splitLines(String aString) {
    return Arrays.asList(aString.split("\n"));
  }
}
