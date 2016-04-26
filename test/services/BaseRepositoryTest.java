package services;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import models.TripleCommit;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import helpers.ElasticsearchTestGrid;
import helpers.JsonLdConstants;
import helpers.JsonTest;
import models.Resource;

public class BaseRepositoryTest extends ElasticsearchTestGrid implements JsonTest {

  private static Map<String, String> mMetadata = new HashMap<>();

  @BeforeClass
  public static void setUp() {
    mMetadata.put(TripleCommit.Header.AUTHOR_HEADER, "Anonymous");
    mMetadata.put(TripleCommit.Header.DATE_HEADER, "2016-04-08T17:34:37.038+02:00");
  }

  @Test
  public void testResourceWithIdentifiedSubObject() throws IOException {
    Resource resource1 = new Resource("Person", "info:id001");
    resource1.put(JsonLdConstants.CONTEXT, "http://schema.org/");
    Resource resource2 = new Resource("Event", "info:OER15");
    resource2.put(JsonLdConstants.CONTEXT, "http://schema.org/");
    resource1.put("attended", resource2);
    Resource expected1 = getResourceFromJsonFile("BaseRepositoryTest/testResourceWithIdentifiedSubObject.OUT.1.json");
    Resource expected2 = getResourceFromJsonFile("BaseRepositoryTest/testResourceWithIdentifiedSubObject.OUT.2.json");
    mBaseRepo.addResource(resource1, mMetadata);
    mBaseRepo.addResource(resource2, mMetadata);
    Assert.assertEquals(expected1, mBaseRepo.getResource("info:id001"));
    Assert.assertEquals(expected2, mBaseRepo.getResource("info:OER15"));
  }

  @Test
  public void testResourceWithUnidentifiedSubObject() throws IOException {
    Resource resource = new Resource("Person", "info:id002");
    resource.put(JsonLdConstants.CONTEXT, "http://schema.org/");
    Resource value = new Resource("Foo", null);
    resource.put("attended", value);
    Resource expected = getResourceFromJsonFile("BaseRepositoryTest/testResourceWithUnidentifiedSubObject.OUT.1.json");
    mBaseRepo.addResource(resource, mMetadata);
    Assert.assertEquals(expected, mBaseRepo.getResource("info:id002"));
  }

  @Test
  public void testDeleteResourceWithMentionedResources() throws IOException, InterruptedException {
    // setup: 1 Person ("in1") who has 2 affiliations
    Resource in1 = getResourceFromJsonFile("BaseRepositoryTest/testDeleteResourceWithMentionedResources.IN.1.json");
    Resource in2 = getResourceFromJsonFile("BaseRepositoryTest/testDeleteResourceWithMentionedResources.IN.2.json");
    Resource in3 = getResourceFromJsonFile("BaseRepositoryTest/testDeleteResourceWithMentionedResources.IN.3.json");
    Resource expected1 = getResourceFromJsonFile(
        "BaseRepositoryTest/testDeleteResourceWithMentionedResources.OUT.1.json");
    Resource expected2 = getResourceFromJsonFile(
        "BaseRepositoryTest/testDeleteResourceWithMentionedResources.OUT.2.json");

    mBaseRepo.addResource(in1, mMetadata);
    mBaseRepo.addResource(in2, mMetadata);
    mBaseRepo.addResource(in3, mMetadata);
    // delete affiliation "Oh No Company" and check whether it has been removed
    // from referencing resources
    Resource toBeDeleted = mBaseRepo.getResource("info:urn:uuid:49d8b330-e3d5-40ca-b5cb-2a8dfca70987");
    // FIXME: Thread.sleep to be deleted when Repo synchronization is
    // triggerable
    Thread.sleep(1000);
    mBaseRepo.deleteResource(toBeDeleted.getAsString(JsonLdConstants.ID), mMetadata);
    Resource result1 = mBaseRepo.getResource("info:urn:uuid:49d8b330-e3d5-40ca-b5cb-2a8dfca70456");
    Resource result2 = mBaseRepo.getResource("info:urn:uuid:49d8b330-e3d5-40ca-b5cb-2a8dfca70123");
    Assert.assertEquals(expected1, result1);
    Assert.assertEquals(expected2, result2);
    Assert.assertNull(mBaseRepo.getResource("info:urn:uuid:49d8b330-e3d5-40ca-b5cb-2a8dfca70987"));
  }

  @Test
  public void testDeleteLastResourceInList() throws IOException, InterruptedException {
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testDeleteLastResourceInList.DB.1.json");
    Resource db2 = getResourceFromJsonFile("BaseRepositoryTest/testDeleteLastResourceInList.DB.2.json");
    Resource out = getResourceFromJsonFile("BaseRepositoryTest/testDeleteLastResourceInList.OUT.1.json");
    mBaseRepo.addResource(db1, mMetadata);
    mBaseRepo.addResource(db2, mMetadata);
    // FIXME: Thread.sleep to be deleted when Repo synchronization is
    // triggerable
    Thread.sleep(1000);
    mBaseRepo.deleteResource("urn:uuid:3a25e950-a3c0-425d-946d-9806665ec665", mMetadata);
    Assert.assertNull(mBaseRepo.getResource("urn:uuid:3a25e950-a3c0-425d-946d-9806665ec665"));
    Assert.assertEquals(out, mBaseRepo.getResource("urn:uuid:c7f5334a-3ddb-4e46-8653-4d8c01e25503"));
  }

  @Test
  public void testDeleteResourceFromList() throws IOException, InterruptedException {
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testDeleteResourceFromList.DB.1.json");
    Resource db2 = getResourceFromJsonFile("BaseRepositoryTest/testDeleteResourceFromList.DB.2.json");
    Resource db3 = getResourceFromJsonFile("BaseRepositoryTest/testDeleteResourceFromList.DB.3.json");
    Resource out1 = getResourceFromJsonFile("BaseRepositoryTest/testDeleteResourceFromList.OUT.1.json");
    Resource out2 = getResourceFromJsonFile("BaseRepositoryTest/testDeleteResourceFromList.OUT.2.json");
    mBaseRepo.addResource(db1, mMetadata);
    mBaseRepo.addResource(db2, mMetadata);
    mBaseRepo.addResource(db3, mMetadata);
    // FIXME: Thread.sleep to be deleted when Repo synchronization is
    // triggerable
    Thread.sleep(1000);
    mBaseRepo.deleteResource("urn:uuid:3a25e950-a3c0-425d-946d-9806665ec665", mMetadata);
    Assert.assertNull(mBaseRepo.getResource("urn:uuid:3a25e950-a3c0-425d-946d-9806665ec665"));
    Assert.assertEquals(out1, mBaseRepo.getResource("urn:uuid:c7f5334a-3ddb-4e46-8653-4d8c01e25503"));
    Assert.assertEquals(out2, mBaseRepo.getResource("urn:uuid:7cfb9aab-1a3f-494c-8fb1-64755faf180c"));
  }

  @Test
  public void testRemoveReference() throws IOException {
    Resource in = getResourceFromJsonFile("BaseRepositoryTest/testRemoveReference.IN.json");
    Resource db1 = getResourceFromJsonFile("BaseRepositoryTest/testRemoveReference.DB.1.json");
    Resource db2 = getResourceFromJsonFile("BaseRepositoryTest/testRemoveReference.DB.2.json");
    Resource out1 = getResourceFromJsonFile("BaseRepositoryTest/testRemoveReference.OUT.1.json");
    Resource out2 = getResourceFromJsonFile("BaseRepositoryTest/testRemoveReference.OUT.2.json");
    mBaseRepo.addResource(db1, mMetadata);
    mBaseRepo.addResource(db2, mMetadata);
    mBaseRepo.addResource(in, mMetadata);
    Resource get1 = mBaseRepo.getResource(out1.getAsString(JsonLdConstants.ID));
    Resource get2 = mBaseRepo.getResource(out2.getAsString(JsonLdConstants.ID));
    assertEquals(out1, get1);
    assertEquals(out2, get2);
  }

}
