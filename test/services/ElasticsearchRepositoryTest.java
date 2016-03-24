package services;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import helpers.ElasticsearchTestGrid;
import helpers.JsonLdConstants;
import helpers.JsonTest;
import models.Resource;
import models.ResourceList;

public class ElasticsearchRepositoryTest extends ElasticsearchTestGrid implements JsonTest {

  private static Resource mResource1;
  private static Resource mResource2;
  private static Resource mResource3;

  @BeforeClass
  public static void setupResources() throws IOException {
    mResource1 = new Resource("Person");
    mResource1.put("name", "oeruser1");
    mResource1.put("worksFor", "oerknowledgecloud.org");

    mResource2 = new Resource("Person", UUID.randomUUID().toString());
    mResource2.put("name", "oeruser2");
    mResource2.put("worksFor", "unesco.org");

    mResource3 = new Resource("Person", UUID.randomUUID().toString());
    mResource3.put("name", "oeruser3");
    mResource3.put("worksFor", "unesco.org");

    mRepo.addResource(mResource1, "Person");
    mRepo.addResource(mResource2, "Person");
    mRepo.addResource(mResource3, "Person");
    mRepo.refreshIndex(mConfig.getString("es.index.name"));
  }

  @Test
  public void testAddAndQueryResources() throws IOException, ParseException {
    List<Resource> resourcesGotBack = mRepo.getAll("Person");
    Assert.assertTrue(resourcesGotBack.contains(mResource1));
    Assert.assertTrue(resourcesGotBack.contains(mResource2));
  }

  @Test
  public void testAddAndEsQueryResources() throws IOException, ParseException {
    final String aQueryString = "Person";
    ResourceList result = null;
    try {
      // TODO : this test currently presumes that there is some data existent in
      // your elasticsearch instance. Otherwise it will fail. This restriction
      // can be overturned when a parallel method for the use of POST is
      // introduced in ElasticsearchRepository.
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
    List<Resource> resourcesGotBack = mRepo.getAll("Person");
    Set<String> ids = new HashSet<String>();
    Set<String> names = new HashSet<String>();
    Set<String> employers = new HashSet<String>();

    for (Resource r : resourcesGotBack) {
      ids.add(r.getAsString(JsonLdConstants.ID));
      names.add(r.get("name").toString());
      employers.add(r.get("worksFor").toString());
    }

    // unique fields: ids and names (in this case shall be unique, i. e.
    // appearing exactly one time per Resource
    Assert.assertTrue(resourcesGotBack.size() == ids.size() && ids.size() == names.size());
    // non-unique fields : some "persons" work for the same employer:
    Assert.assertTrue(resourcesGotBack.size() > employers.size());
  }

}
