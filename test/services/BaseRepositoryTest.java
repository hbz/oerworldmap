package services;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import helpers.ElasticsearchTestGrid;
import helpers.JsonTest;
import models.Resource;

public class BaseRepositoryTest extends ElasticsearchTestGrid implements JsonTest {

  @Test
  public void testResourceWithIdentifiedSubObject() throws IOException {
    Resource resource = new Resource("Person", "id001");
    String property = "attended";
    Resource value = new Resource("Event", "OER15");
    resource.put(property, value);
    Resource expected1 = getResourceFromJsonFile(
        "BaseRepositoryTest/testResourceWithIdentifiedSubObject.OUT.1.json");
    Resource expected2 = getResourceFromJsonFile(
        "BaseRepositoryTest/testResourceWithIdentifiedSubObject.OUT.2.json");
    mBaseRepo.addResource(resource);
    Assert.assertEquals(expected1, mBaseRepo.getResource("id001"));
    Assert.assertEquals(expected2, mBaseRepo.getResource("OER15"));
  }

  @Test
  public void testResourceWithUnidentifiedSubObject() throws IOException {
    Resource resource = new Resource("Person", "id002");
    Resource value = new Resource("Foo", null);
    resource.put("attended", value);
    Resource expected = getResourceFromJsonFile(
        "BaseRepositoryTest/testResourceWithUnidentifiedSubObject.OUT.1.json");
    mBaseRepo.addResource(resource);
    Assert.assertEquals(expected, mBaseRepo.getResource("id002"));
  }

}
