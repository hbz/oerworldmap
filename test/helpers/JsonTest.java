package helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import models.Resource;
import models.ResourceList;
import play.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface JsonTest {

  default Resource getResourceFromJsonFile(String aFile) throws IOException {
    InputStream in = ClassLoader.getSystemResourceAsStream(aFile);
    return new Resource(new ObjectMapper().readTree(in));
  }

  default Resource getResourceFromJsonFileUnsafe(String aFile) {
    InputStream in = ClassLoader.getSystemResourceAsStream(aFile);
    try {
      return new Resource(new ObjectMapper().readTree(in));
    } catch (IOException e) {
      Logger.error(e.toString());
      return null;
    }
  }

  default ResourceList getResourcesFromPagedCollectionFile(String aPagedCollectionFile) throws IOException {
    InputStream in = ClassLoader.getSystemResourceAsStream(aPagedCollectionFile);
    return new ResourceList(new Resource(new ObjectMapper().readTree(in)));
  }

  default List<Resource> getResourcesFromJsonDir(String aDir) throws IOException {
    List<Resource> resources = new ArrayList<>();
    try {
      URL pathURL = ClassLoader.getSystemResource(aDir);
      if ((pathURL != null) && pathURL.getProtocol().equals("file")) {
        for (String file : new File(pathURL.toURI()).list()) {
          if (file.endsWith(".json")) {
            InputStream in = ClassLoader.getSystemResourceAsStream(aDir.concat(file));
            resources.add(new Resource(new ObjectMapper().readTree(in)));
          }
        }
      }
    } catch (URISyntaxException e) {
      Logger.error(e.toString());
    }
    return resources;
  }

  default String getStringFromFile(String aPath, Charset aEncoding)
    throws IOException
  {
    byte[] encoded = Files.readAllBytes(Paths.get(ClassLoader.getSystemResource(aPath).toExternalForm().substring(5)));
    return new String(encoded, aEncoding);
  }

  default List<String> splitLines(String aString){
    return Arrays.asList(aString.split("\n"));
  }

}
