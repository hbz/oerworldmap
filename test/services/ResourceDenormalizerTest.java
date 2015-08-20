package services;

import static org.junit.Assert.assertEquals;

import helpers.JsonLdConstants;
import helpers.JsonTest;

import java.io.IOException;
import java.util.List;

import models.Resource;

import org.junit.Test;

/**
 * @author fo, pvb
 */
public class ResourceDenormalizerTest implements JsonTest{

  @Test
  public void testNewResourceWithNewReference() throws IOException {
    Resource in = getResourceFromJsonFile("ResourceDenormalizerTest/testNewResourceWithNewReference.IN.json");
    Resource out1 = getResourceFromJsonFile("ResourceDenormalizerTest/testNewResourceWithNewReference.OUT.1.json");
    Resource out2 = getResourceFromJsonFile("ResourceDenormalizerTest/testNewResourceWithNewReference.OUT.2.json");
    MockResourceRepository repo = new MockResourceRepository();
    List<Resource> denormalized = ResourceDenormalizer.denormalize(in, repo);
    for (Resource resource : denormalized){
      repo.addResource(resource);
    }
    assertEquals(2, repo.size());
    Resource get1 = repo.getResource(out1.getAsString(JsonLdConstants.ID));
    Resource get2 = repo.getResource(out2.getAsString(JsonLdConstants.ID));
    assertEquals(out1, get1);
    assertEquals(out2, get2);
  }

  @Test
  public void testNewResourceWithExistingReference() throws IOException {
    Resource in = getResourceFromJsonFile("ResourceDenormalizerTest/testNewResourceWithExistingReference.IN.json");
    Resource db = getResourceFromJsonFile("ResourceDenormalizerTest/testNewResourceWithExistingReference.DB.1.json");
    Resource out1 = getResourceFromJsonFile("ResourceDenormalizerTest/testNewResourceWithExistingReference.OUT.1.json");
    Resource out2 = getResourceFromJsonFile("ResourceDenormalizerTest/testNewResourceWithExistingReference.OUT.2.json");
    MockResourceRepository repo = new MockResourceRepository();
    List<Resource> denormalizedDb = ResourceDenormalizer.denormalize(db, repo);
    for (Resource resource : denormalizedDb){
      repo.addResource(resource);
    }
    List<Resource> denormalizedIn = ResourceDenormalizer.denormalize(in, repo);
    for (Resource resource : denormalizedIn){
      repo.addResource(resource);
    }
    assertEquals(3, repo.size());
    Resource get1 = repo.getResource(out1.getAsString(JsonLdConstants.ID));
    Resource get2 = repo.getResource(out2.getAsString(JsonLdConstants.ID));
    assertEquals(out1, get1);
    assertEquals(out2, get2);
  }
  
  @Test
  public void testModifyExistingResource() throws IOException {
    Resource in = getResourceFromJsonFile("ResourceDenormalizerTest/testModifyExistingResource.IN.json");
    Resource db1 = getResourceFromJsonFile("ResourceDenormalizerTest/testModifyExistingResource.DB.1.json");
    Resource db2 = getResourceFromJsonFile("ResourceDenormalizerTest/testModifyExistingResource.DB.2.json");
    Resource db3 = getResourceFromJsonFile("ResourceDenormalizerTest/testModifyExistingResource.DB.3.json");
    Resource out1 = getResourceFromJsonFile("ResourceDenormalizerTest/testModifyExistingResource.OUT.1.json");
    Resource out2 = getResourceFromJsonFile("ResourceDenormalizerTest/testModifyExistingResource.OUT.2.json");
    Resource out3 = getResourceFromJsonFile("ResourceDenormalizerTest/testModifyExistingResource.OUT.3.json");
    MockResourceRepository repo = new MockResourceRepository();
    repo.addResource(db1);
    repo.addResource(db2);
    repo.addResource(db3);
    List<Resource> denormalized = ResourceDenormalizer.denormalize(in, repo);
    for (Resource resource : denormalized){
      repo.addResource(resource);
    }
    assertEquals(3, repo.size());
    Resource get1 = repo.getResource(out1.getAsString(JsonLdConstants.ID));
    Resource get2 = repo.getResource(out2.getAsString(JsonLdConstants.ID));
    Resource get3 = repo.getResource(out3.getAsString(JsonLdConstants.ID));
    assertEquals(out1, get1);
    assertEquals(out2, get2);
    assertEquals(out3, get3);
  }

  @Test
  public void testModifyExistingResourceAllReferences() throws IOException {
    Resource in = getResourceFromJsonFile("ResourceDenormalizerTest/testModifyExistingResourceAllReferences.IN.json");
    Resource db1 = getResourceFromJsonFile("ResourceDenormalizerTest/testModifyExistingResourceAllReferences.DB.1.json");
    Resource db2 = getResourceFromJsonFile("ResourceDenormalizerTest/testModifyExistingResourceAllReferences.DB.2.json");
    Resource db3 = getResourceFromJsonFile("ResourceDenormalizerTest/testModifyExistingResourceAllReferences.DB.3.json");
    Resource db4 = getResourceFromJsonFile("ResourceDenormalizerTest/testModifyExistingResourceAllReferences.DB.4.json");
    Resource out1 = getResourceFromJsonFile("ResourceDenormalizerTest/testModifyExistingResourceAllReferences.OUT.1.json");
    Resource out2 = getResourceFromJsonFile("ResourceDenormalizerTest/testModifyExistingResourceAllReferences.OUT.2.json");
    Resource out3 = getResourceFromJsonFile("ResourceDenormalizerTest/testModifyExistingResourceAllReferences.OUT.3.json");
    Resource out4 = getResourceFromJsonFile("ResourceDenormalizerTest/testModifyExistingResourceAllReferences.OUT.4.json");
    MockResourceRepository repo = new MockResourceRepository();
    repo.addResource(db1);
    repo.addResource(db2);
    repo.addResource(db3);
    repo.addResource(db4);
    List<Resource> denormalized = ResourceDenormalizer.denormalize(in, repo);
    for (Resource resource : denormalized){
      repo.addResource(resource);
    }
    assertEquals(4, repo.size());
    Resource get1 = repo.getResource(out1.getAsString(JsonLdConstants.ID));
    Resource get2 = repo.getResource(out2.getAsString(JsonLdConstants.ID));
    Resource get3 = repo.getResource(out3.getAsString(JsonLdConstants.ID));
    Resource get4 = repo.getResource(out4.getAsString(JsonLdConstants.ID));
    assertEquals(out1, get1);
    assertEquals(out2, get2);
    assertEquals(out3, get3);
    assertEquals(out4, get4);
  }

  @Test
  public void testRemoveReference() throws IOException {
    Resource in = getResourceFromJsonFile("ResourceDenormalizerTest/testRemoveReference.IN.json");
    Resource db1 = getResourceFromJsonFile("ResourceDenormalizerTest/testRemoveReference.DB.1.json");
    Resource db2 = getResourceFromJsonFile("ResourceDenormalizerTest/testRemoveReference.DB.2.json");
    Resource out1 = getResourceFromJsonFile("ResourceDenormalizerTest/testRemoveReference.OUT.1.json");
    Resource out2 = getResourceFromJsonFile("ResourceDenormalizerTest/testRemoveReference.OUT.2.json");
    MockResourceRepository repo = new MockResourceRepository();
    repo.addResource(db1);
    repo.addResource(db2);
    List<Resource> denormalized = ResourceDenormalizer.denormalize(in, repo);
    for (Resource resource : denormalized){
      repo.addResource(resource);
    }
    assertEquals(2, repo.size());
    Resource get1 = repo.getResource(out1.getAsString(JsonLdConstants.ID));
    Resource get2 = repo.getResource(out2.getAsString(JsonLdConstants.ID));
    assertEquals(out1, get1);
    assertEquals(out2, get2);
  }

  @Test
  public void testResourceWithUnidentifiedObjectValues() throws IOException {

    Resource original = getResourceFromJsonFile("ResourceDenormalizerTest/testResourceWithUnidentifiedObjectValues.IN.json");
    Resource in = getResourceFromJsonFile("ResourceDenormalizerTest/testResourceWithUnidentifiedObjectValues.IN.json");
    ResourceRepository repo = new MockResourceRepository();

    // Test case: empty repo
    List<Resource> denormalized = ResourceDenormalizer.denormalize(in, repo);
    assertEquals(1, denormalized.size());
    assertEquals(original, denormalized.get(0));

    // Test case: resource already in repo
    repo.addResource(in);
    denormalized = ResourceDenormalizer.denormalize(in, repo);
    assertEquals(1, denormalized.size());
    assertEquals(original, denormalized.get(0));

  }

  @Test
  public void testResourceIsUpdatedWithNewUnidentifiedValuesOnly() throws IOException {
    Resource in = getResourceFromJsonFile("ResourceDenormalizerTest/testResourceIsUpdatedWithNewUnidentifiedValuesOnly.IN.json");
    Resource db = getResourceFromJsonFile("ResourceDenormalizerTest/testResourceIsUpdatedWithNewUnidentifiedValuesOnly.DB.json");
    ResourceRepository repo = new MockResourceRepository();
    repo.addResource(db);
    List<Resource> denormalized = ResourceDenormalizer.denormalize(in, repo);
    assertEquals(1, denormalized.size());
    assertEquals(in, denormalized.get(0));
  }

}
