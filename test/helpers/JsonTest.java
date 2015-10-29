package helpers;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import models.Resource;

public interface JsonTest {

  default public Resource getResourceFromJsonFile(String aFile) throws IOException {
    InputStream in = ClassLoader.getSystemResourceAsStream(aFile);
    String json = IOUtils.toString(in, "UTF-8");
    Resource person = Resource.fromJson(json);
    return person;
  }

}