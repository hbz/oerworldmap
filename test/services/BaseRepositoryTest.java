package services;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import helpers.ElasticsearchTestGrid;
import helpers.JsonLdConstants;
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

  @Test
  public void testDeleteResourceWithMentionedResources() throws IOException {
    // setup: 1 Person ("in1") who has 2 affiliations
    Resource in = getResourceFromJsonFile(
        "BaseRepositoryTest/testDeleteResourceWithMentionedResources.IN.1.json");
    Resource expected1 = getResourceFromJsonFile(
        "BaseRepositoryTest/testDeleteResourceWithMentionedResources.OUT.1.json");
    Resource expected2 = getResourceFromJsonFile(
        "BaseRepositoryTest/testDeleteResourceWithMentionedResources.OUT.2.json");
    List<Resource> denormalized = ResourceDenormalizer.denormalize(in, mBaseRepo);

    for (Resource resource : denormalized) {
      mBaseRepo.addResource(resource);
    }
    // delete affiliation "Oh No Company" and check whether it has been removed
    // from referencing resources
    Resource toBeDeleted = mBaseRepo.getResource("9m8n7b");
    mBaseRepo.deleteResource(toBeDeleted.getAsString(JsonLdConstants.ID));
    Resource result1 = mBaseRepo.getResource("4g5h6j");
    Resource result2 = mBaseRepo.getResource("1a2s3d");
    Assert.assertEquals(expected1, result1);
    Assert.assertEquals(expected2, result2);
    Assert.assertNull(mBaseRepo.getResource("9m8n7b"));
  }

  @Test
  public void testDeleteLastResourceInList() throws IOException {
    Resource db1 = getResourceFromJsonFile(
      "BaseRepositoryTest/testDeleteLastResourceInList.DB.1.json");
    Resource db2 = getResourceFromJsonFile(
      "BaseRepositoryTest/testDeleteLastResourceInList.DB.2.json");
    Resource out = getResourceFromJsonFile(
      "BaseRepositoryTest/testDeleteLastResourceInList.OUT.1.json");
    mBaseRepo.addResource(db1);
    mBaseRepo.addResource(db2);
    mBaseRepo.deleteResource("urn:uuid:3a25e950-a3c0-425d-946d-9806665ec665");
    Assert.assertNull(mBaseRepo.getResource("urn:uuid:3a25e950-a3c0-425d-946d-9806665ec665"));
    Assert.assertEquals(out, mBaseRepo.getResource("urn:uuid:c7f5334a-3ddb-4e46-8653-4d8c01e25503"));
  }

  @Test
  public void testDeleteResourceFromList() throws IOException {
    Resource db1 = getResourceFromJsonFile(
      "BaseRepositoryTest/testDeleteResourceFromList.DB.1.json");
    Resource db2 = getResourceFromJsonFile(
      "BaseRepositoryTest/testDeleteResourceFromList.DB.2.json");
    Resource db3 = getResourceFromJsonFile(
      "BaseRepositoryTest/testDeleteResourceFromList.DB.2.json");
    Resource out1 = getResourceFromJsonFile(
      "BaseRepositoryTest/testDeleteResourceFromList.OUT.1.json");
    Resource out2 = getResourceFromJsonFile(
      "BaseRepositoryTest/testDeleteResourceFromList.OUT.2.json");
    mBaseRepo.addResource(db1);
    mBaseRepo.addResource(db2);
    mBaseRepo.addResource(db3);
    mBaseRepo.deleteResource("urn:uuid:3a25e950-a3c0-425d-946d-9806665ec665");
    Assert.assertNull(mBaseRepo.getResource("urn:uuid:3a25e950-a3c0-425d-946d-9806665ec665"));
    Assert.assertEquals(out1, mBaseRepo.getResource("urn:uuid:c7f5334a-3ddb-4e46-8653-4d8c01e25503"));
    Assert.assertEquals(out2, mBaseRepo.getResource("urn:uuid:7cfb9aab-1a3f-494c-8fb1-64755faf180c"));
  }

  @Test
  public void testGetResourcesWithWildcard() throws IOException {
    Resource in1 = getResourceFromJsonFile(
        "BaseRepositoryTest/testGetResourcesWithWildcard.DB.1.json");
    Resource in2 = getResourceFromJsonFile(
        "BaseRepositoryTest/testGetResourcesWithWildcard.DB.2.json");
    mBaseRepo.addResource(in1);
    mBaseRepo.addResource(in2);
    Assert.assertEquals(2, mBaseRepo.getResources("\\*.@id", "123").size());
  }

}
