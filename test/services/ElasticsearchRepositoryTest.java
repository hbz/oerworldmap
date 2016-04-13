package services;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Test;

import helpers.ElasticsearchTestGrid;
import helpers.JsonLdConstants;
import helpers.JsonTest;
import models.Resource;
import models.ResourceList;

public class ElasticsearchRepositoryTest extends ElasticsearchTestGrid implements JsonTest {

  @Test
  public void testAddAndQueryResources() throws IOException, ParseException {
    Resource in1 = getResourceFromJsonFile(
      "BaseRepositoryTest/testGetResourcesWithWildcard.DB.1.json");
    Resource in2 = getResourceFromJsonFile(
      "BaseRepositoryTest/testGetResourcesWithWildcard.DB.2.json");
    mRepo.addResource(in1, new HashMap<>());
    mRepo.addResource(in2, new HashMap<>());

    List<Resource> resourcesGotBack = mRepo.getAll("Person");
    Assert.assertTrue(resourcesGotBack.contains(in1));
    Assert.assertFalse(resourcesGotBack.contains(in2));
  }

  @Test
  public void testAddAndEsQueryResources() throws IOException, ParseException {
    Resource in1 = getResourceFromJsonFile(
      "BaseRepositoryTest/testGetResourcesWithWildcard.DB.1.json");
    Resource in2 = getResourceFromJsonFile(
      "BaseRepositoryTest/testGetResourcesWithWildcard.DB.2.json");
    mRepo.addResource(in1, new HashMap<>());
    mRepo.addResource(in2, new HashMap<>());
    final String aQueryString = "*";
    ResourceList result = null;
    try {
      result = mRepo.query(aQueryString, 0, 10, null, null);
    } catch (IOException | ParseException e) {
      e.printStackTrace();
    } finally {
      Assert.assertNotNull(result);
      Assert.assertTrue(!result.getItems().isEmpty());
    }
  }

  @Test
  public void testUniqueFields() throws IOException, ParseException {
    Resource in1 = getResourceFromJsonFile(
      "BaseRepositoryTest/testGetResourcesWithWildcard.DB.1.json");
    Resource in2 = getResourceFromJsonFile(
      "BaseRepositoryTest/testGetResourcesWithWildcard.DB.2.json");
    mRepo.addResource(in1, new HashMap<>());
    mRepo.addResource(in2, new HashMap<>());
    List<Resource> resourcesGotBack = mRepo.getAll("Person");
    Set<String> ids = new HashSet<String>();
    Set<String> names = new HashSet<String>();

    for (Resource r : resourcesGotBack) {
      ids.add(r.getAsString(JsonLdConstants.ID));
      names.add(r.get("name").toString());
    }

    // unique fields: ids and names (in this case shall be unique, i. e.
    // appearing exactly one time per Resource
    Assert.assertTrue(resourcesGotBack.size() == ids.size() && ids.size() == names.size());

  }

  @Test
  public void testGetResourcesWithWildcard() throws IOException {
    Resource in1 = getResourceFromJsonFile(
      "BaseRepositoryTest/testGetResourcesWithWildcard.DB.1.json");
    Resource in2 = getResourceFromJsonFile(
      "BaseRepositoryTest/testGetResourcesWithWildcard.DB.2.json");
    mRepo.addResource(in1, new HashMap<>());
    mRepo.addResource(in2, new HashMap<>());
    Assert.assertEquals(2, mRepo.getResources("\\*.@id", "123").size());
  }

}
