package schema;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import helpers.JsonTest;

import java.io.IOException;

import models.Resource;

import org.junit.Test;


/**
 * @author fo
 */
public class SchemaTest implements JsonTest{

  @Test
  public void testPerson() throws IOException {
    Resource person = getResourceFromJsonFile("resources/SchemaTest/testPerson.json");
    assertNotNull(person);
    assertTrue(person.validate().isSuccess());
  }

  @Test
  public void testService() throws IOException {
    Resource service = getResourceFromJsonFile("resources/SchemaTest/testService.json");
    assertNotNull(service);
    assertTrue(service.validate().isSuccess());
  }

  @Test
  public void testArticle() throws IOException {
    Resource article = getResourceFromJsonFile("resources/SchemaTest/testArticle.json");
    assertNotNull(article);
    assertTrue(article.validate().isSuccess());
  }

}
