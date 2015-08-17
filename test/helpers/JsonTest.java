package helpers;

import java.io.IOException;
import java.io.InputStream;

import models.Resource;

import org.apache.commons.io.IOUtils;

public interface JsonTest {
  
  default public Resource getResourceFromJsonFile(String aFile) throws IOException {
    InputStream in = ClassLoader.getSystemResourceAsStream(aFile);
    String json = IOUtils.toString(in, "UTF-8");
    Resource person = Resource.fromJson(json);
    return person;
  }

}
