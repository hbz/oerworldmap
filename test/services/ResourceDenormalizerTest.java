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
    Resource in = getResourceFromJsonFile("resources/ResourceDenormalizerTest/testNewResourceWithNewReference.IN.json");
    Resource out1 = getResourceFromJsonFile("resources/ResourceDenormalizerTest/testNewResourceWithNewReference.OUT.1.json");
    Resource out2 = getResourceFromJsonFile("resources/ResourceDenormalizerTest/testNewResourceWithNewReference.OUT.2.json");
    MockResourceRepository repo = new MockResourceRepository();
    List<Resource> denormalized = ResourceDenormalizer.denormalize(in, repo);
    for (Resource resource : denormalized){
      repo.addResource(resource);
    }
    assertEquals(2, repo.size());
    Resource get1 = repo.getResource(out1.get(JsonLdConstants.ID).toString());
    Resource get2 = repo.getResource(out2.get(JsonLdConstants.ID).toString());
    assertEquals(out1, get1);
    assertEquals(out2, get2);
  }

  // TODO: not yet working @Test 
  public void testNewResourceWithExistingReference() throws IOException {
    Resource in = getResourceFromJsonFile("resources/ResourceDenormalizerTest/testNewResourceWithExistingReference.IN.json");
    Resource db = getResourceFromJsonFile("resources/ResourceDenormalizerTest/testNewResourceWithExistingReference.DB.1.json");
    Resource out1 = getResourceFromJsonFile("resources/ResourceDenormalizerTest/testNewResourceWithExistingReference.OUT.1.json");
    Resource out2 = getResourceFromJsonFile("resources/ResourceDenormalizerTest/testNewResourceWithExistingReference.OUT.2.json");
    MockResourceRepository repo = new MockResourceRepository();
    repo.addResource(db);
    List<Resource> denormalized = ResourceDenormalizer.denormalize(in, repo);
    for (Resource resource : denormalized){
      repo.addResource(resource);
    }
    assertEquals(2, repo.size());
    Resource get1 = repo.getResource(out1.get(JsonLdConstants.ID).toString());
    Resource get2 = repo.getResource(out2.get(JsonLdConstants.ID).toString());
    assertEquals(out1, get1);
    assertEquals(out2, get2);
  }

  @Test
  public void testModifyExistingResource() {
    // testModifyExistingResource.IN.json
    // testModifyExistingResource.DB.1.json
    // testModifyExistingResource.DB.2.json
    // testModifyExistingResource.DB.3.json
    // testModifyExistingResource.OUT.1.json
    // testModifyExistingResource.OUT.2.json
    // testModifyExistingResource.OUT.3.json
  }

  @Test
  public void testModifyExistingResourceAllReferences() {
    // testModifyExistingResourceAllReferences.IN.json
    // testModifyExistingResourceAllReferences.DB.1.json
    // testModifyExistingResourceAllReferences.DB.2.json
    // testModifyExistingResourceAllReferences.DB.3.json
    // testModifyExistingResourceAllReferences.DB.4.json
    // testModifyExistingResourceAllReferences.OUT.1.json
    // testModifyExistingResourceAllReferences.OUT.2.json
    // testModifyExistingResourceAllReferences.OUT.3.json
    // testModifyExistingResourceAllReferences.OUT.4.json
  }
}
