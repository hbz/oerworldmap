package oerworldmap;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.File;
import java.io.IOException;
import java.util.List;

import models.Resource;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import services.ElasticsearchClient;
import services.ElasticsearchRepository;

public class ElasticsearchRepositoryTest {

  private static Resource mResource1;
  private static Resource mResource2;
  private static ElasticsearchClient mElClient;
  private static ElasticsearchRepository mRepo;
  public static final Config CONFIG = ConfigFactory.parseFile(new File("conf/application.conf"))
      .resolve();
  private static final String ES_INDEX = CONFIG.getString("es.index.name");
  private static final String ES_TYPE = CONFIG.getString("es.index.type");
  private static final String ES_CLUSTER = CONFIG.getString("es.cluster.name");

  private static final Builder CLIENT_SETTINGS = ImmutableSettings.settingsBuilder()
      .put("index.name", ES_INDEX).put("index.type", ES_TYPE).put("cluster.name", ES_CLUSTER);

  @BeforeClass
  public static void setup() throws IOException {
    mResource1 = new Resource();
    mResource1.addProperty("name", "oeruser1");
    mResource1.addProperty("worksFor", "oerknowledgecloud.org");
    mResource1.addProperty("type", ES_TYPE);

    mResource2 = new Resource();
    mResource2.addProperty("name", "oeruser2");
    mResource2.addProperty("worksFor", "unesco.org");
    mResource2.addProperty("type", ES_TYPE);

    mElClient = new ElasticsearchClient(nodeBuilder().settings(CLIENT_SETTINGS).local(true).node()
        .client());
    cleanIndex();
    mRepo = new ElasticsearchRepository(mElClient);
  }

  // create a new clean ElasticsearchIndex for this Test class
  private static void cleanIndex() {
    mElClient.getClient().admin().indices().delete(new DeleteIndexRequest(ES_INDEX)).actionGet();
    mElClient.getClient().admin().indices().create(new CreateIndexRequest(ES_INDEX)).actionGet();
  }

  @Test
  public void testAddAndQueryResources() {
    mRepo.addResource(mResource1);
    mRepo.addResource(mResource2);
    mElClient.getClient().admin().indices().refresh(new RefreshRequest(ES_INDEX)).actionGet();

    List<Resource> resourcesGotBack = mRepo.queryAll(ES_TYPE);

    Assert.assertTrue(resourcesGotBack.contains(mResource1));
    Assert.assertTrue(resourcesGotBack.contains(mResource2));
  }
}
