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
    Resource person = getResourceFromJsonFile("SchemaTest/testPerson.json");
    assertNotNull(person);
    assertTrue(person.validate().isSuccess());
  }

  @Test
  public void testService() throws IOException {
    Resource service = getResourceFromJsonFile("SchemaTest/testService.json");
    assertNotNull(service);
    assertTrue(service.validate().isSuccess());
  }

  @Test
  public void testArticle() throws IOException {
    Resource article = getResourceFromJsonFile("SchemaTest/testArticle.json");
    assertNotNull(article);
    assertTrue(article.validate().isSuccess());
  }

  @Test
  public void testOrganization() throws IOException {
    Resource organization = getResourceFromJsonFile("SchemaTest/testOrganization.json");
    assertNotNull(organization);
    assertTrue(organization.validate().isSuccess());
  }

  @Test
  public void testEvent() throws IOException {
    Resource event = getResourceFromJsonFile("SchemaTest/testEvent.json");
    assertNotNull(event);
    assertTrue(event.validate().isSuccess());
  }

}
