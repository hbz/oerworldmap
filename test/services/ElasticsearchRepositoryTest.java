package services;

import helpers.ElasticsearchTestGrid;
import helpers.JsonLdConstants;
import helpers.JsonTest;
import helpers.ResourceHelpers;
import models.Resource;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import services.repository.ElasticsearchRepository;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ElasticsearchRepositoryTest extends ElasticsearchTestGrid implements JsonTest {

  private static ElasticsearchRepository mElasticsearchRepo = new ElasticsearchRepository(mConfig);

  private final String[] mIndices = new String[]{"es.index.webpage.name"};

  @BeforeClass
  public static void setupResources() throws IOException {
    mElasticsearchRepo.deleteIndex(mConfig.getString("es.index.webpage.name"));
    mElasticsearchRepo.createIndex(mConfig.getString("es.index.webpage.name"));
  }

  @Test
  public void testAddAndQueryResources() throws IOException {
    Resource in1 = getResourceFromJsonFile(
      "BaseRepositoryTest/testGetResourcesWithWildcard.DB.1.json");
    Resource in2 = getResourceFromJsonFile(
      "BaseRepositoryTest/testGetResourcesWithWildcard.DB.2.json");
    mElasticsearchRepo.addResource(in1, new HashMap<>());
    mElasticsearchRepo.addResource(in2, new HashMap<>());

    List<Resource> resourcesGotBack = ResourceHelpers.unwrapRecords(
      mElasticsearchRepo.getAll("Person", mIndices));
    Assert.assertTrue(resourcesGotBack.contains(in1));
    Assert.assertFalse(resourcesGotBack.contains(in2));
  }

  @Test
  public void testUniqueFields() throws IOException {
    Resource in1 = getResourceFromJsonFile(
      "BaseRepositoryTest/testGetResourcesWithWildcard.DB.1.json");
    Resource in2 = getResourceFromJsonFile(
      "BaseRepositoryTest/testGetResourcesWithWildcard.DB.2.json");
    mElasticsearchRepo.addResource(in1, new HashMap<>());
    mElasticsearchRepo.addResource(in2, new HashMap<>());
    List<Resource> resourcesGotBack = ResourceHelpers.unwrapRecords(
      mElasticsearchRepo.getAll("Person", mIndices));
    Set<String> ids = new HashSet<String>();
    Set<String> names = new HashSet<String>();
    Set<String> employers = new HashSet<String>();

    for (Resource r : resourcesGotBack) {
      ids.add(r.getAsString(JsonLdConstants.ID));
      if (r.get("name") != null) {
        names.add(r.get("name").toString());
      }
      if (r.get("worksFor") != null) {
        employers.add(r.get("worksFor").toString());
      }
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
    mElasticsearchRepo.addResource(in1, new HashMap<>());
    mElasticsearchRepo.addResource(in2, new HashMap<>());
    Assert.assertEquals(2, mElasticsearchRepo.getResources("\\*.@id", "info:123", mIndices).size());
  }

}
