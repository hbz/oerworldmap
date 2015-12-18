package helpers;

import java.io.IOException;
import java.io.InputStream;

import models.Resource;

import org.apache.commons.io.IOUtils;
import play.Logger;

public interface JsonTest {

  default public Resource getResourceFromJsonFile(String aFile) throws IOException {
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

}
