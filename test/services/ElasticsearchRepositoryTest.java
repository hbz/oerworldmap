package services;

import helpers.JsonLdConstants;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import models.Resource;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ElasticsearchRepositoryTest {

  private static Resource mResource1;
  private static Resource mResource2;
  private static Resource mResource3;
  private static Settings mClientSettings;
  private static Client mClient;
  private static ElasticsearchClient mElClient;
  private static ElasticsearchRepository mRepo;
  private static final ElasticsearchConfig esConfig = new ElasticsearchConfig();

  @SuppressWarnings("resource")
  @BeforeClass
  public static void setup() throws IOException {
    ElasticsearchConfig conf = new ElasticsearchConfig();
    mClientSettings = ImmutableSettings.settingsBuilder().put(conf.getClientSettings()).build();
    mClient = new TransportClient(mClientSettings)
        .addTransportAddress(new InetSocketTransportAddress(conf.getServer(), Integer.valueOf(conf
            .getJavaPort())));
    mElClient = new ElasticsearchClient(mClient);
    cleanIndex();
    mRepo = new ElasticsearchRepository(mElClient);

    mResource1 = new Resource(esConfig.getType(), UUID.randomUUID().toString());
    mResource1.put("name", "oeruser1");
    mResource1.put("worksFor", "oerknowledgecloud.org");

    mResource2 = new Resource(esConfig.getType(), UUID.randomUUID().toString());
    mResource2.put("name", "oeruser2");
    mResource2.put("worksFor", "unesco.org");

    mResource3 = new Resource(esConfig.getType(), UUID.randomUUID().toString());
    mResource3.put("name", "oeruser3");
    mResource3.put("worksFor", "unesco.org");

    mRepo.addResource(mResource1);
    mRepo.addResource(mResource2);
    mRepo.addResource(mResource3);
    mElClient.refreshIndex(esConfig.getIndex());
  }

  // create a new clean ElasticsearchIndex for this Test class
  private static void cleanIndex() {
    if (mElClient.hasIndex(esConfig.getIndex())) {
      mElClient.deleteIndex(esConfig.getIndex());
    }
    mElClient.createIndex(esConfig.getIndex());
  }

  @Test
  public void testAddAndQueryResources() throws IOException {
    List<Resource> resourcesGotBack = mRepo.query(esConfig.getType());

    Assert.assertTrue(resourcesGotBack.contains(mResource1));
    Assert.assertTrue(resourcesGotBack.contains(mResource2));
  }

  @Test
  public void testAddAndEsQueryResources() throws IOException {
    final String aQueryString = "_search?@*:*";
    List<Resource> result = null;
    try {
      // TODO : this test currently presumes that there is some data existent in
      // your elasticsearch
      // instance. Otherwise it will fail. This restriction can be overturned
      // when a parallel method
      // for the use of POST is introduced in ElasticsearchRepository.
      result = mRepo.esQuery(aQueryString);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ParseException e) {
      e.printStackTrace();
    } finally {
      Assert.assertNotNull(result);
      Assert.assertTrue(!result.isEmpty());
    }
  }

  @Test
  public void testUniqueFields() throws IOException {
    List<Resource> resourcesGotBack = mRepo.query(esConfig.getType());

    Set<String> ids = new HashSet<String>();
    Set<String> names = new HashSet<String>();
    Set<String> employers = new HashSet<String>();

    for (Resource r : resourcesGotBack) {
      ids.add(r.get(JsonLdConstants.ID).toString());
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
