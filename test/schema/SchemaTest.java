package schema;

import static org.junit.Assert.*;

import models.Resource;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;


/**
 * @author fo
 */
public class SchemaTest {

  @Test
  public void testPerson() throws IOException {
    InputStream in = ClassLoader.getSystemResourceAsStream("SchemaTest/testPerson.json");
    String json = IOUtils.toString(in, "UTF-8");
    Resource person = Resource.fromJson(json);
    assertNotNull(person);
    assertTrue(person.validate().isSuccess());
  }

  @Test
  public void testService() throws IOException {
    InputStream in = ClassLoader.getSystemResourceAsStream("SchemaTest/testService.json");
    String json = IOUtils.toString(in, "UTF-8");
    Resource service = Resource.fromJson(json);
    assertNotNull(service);
    assertTrue(service.validate().isSuccess());
  }

  @Test
  public void testArticle() throws IOException {
    InputStream in = ClassLoader.getSystemResourceAsStream("SchemaTest/testArticle.json");
    String json = IOUtils.toString(in, "UTF-8");
    Resource article = Resource.fromJson(json);
    assertNotNull(article);
    assertTrue(article.validate().isSuccess());
  }

}
