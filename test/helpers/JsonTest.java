package helpers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import models.Resource;

import org.apache.commons.io.IOUtils;
import play.Logger;

public interface JsonTest {

  default Resource getResourceFromJsonFile(String aFile) throws IOException {
    InputStream in = ClassLoader.getSystemResourceAsStream(aFile);
    String json = IOUtils.toString(in, "UTF-8");
    return Resource.fromJson(json);
  }

  default public Resource getResourceFromJsonFileUnsafe(String aFile) {
    InputStream in = ClassLoader.getSystemResourceAsStream(aFile);
    try {
      String json = IOUtils.toString(in, "UTF-8");
      return Resource.fromJson(json);
    } catch (IOException e) {
      Logger.error(e.toString());
      return new Resource();
    }
  }

  default List<Resource> getResourcesFromJsonDir(String aDir) throws IOException {
    List<Resource> resources = new ArrayList<>();
    try {
      URL pathURL = ClassLoader.getSystemResource(aDir);
      if ((pathURL != null) && pathURL.getProtocol().equals("file")) {
        for (String file : new File(pathURL.toURI()).list()) {
          if (file.endsWith(".json")) {
            InputStream in = ClassLoader.getSystemResourceAsStream(aDir.concat(file));
            String json = IOUtils.toString(in, "UTF-8");
            resources.add(Resource.fromJson(json));
          }
        }
      }
    } catch (URISyntaxException e) {
      Logger.error(e.toString());
    }
    return resources;
  }

}
