package assets;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.node.Node;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import assets.ElasticsearchClient;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class ElasticsearchClientTest {
  protected static Client mClient;
  protected static Node mNode;
  protected static ElasticsearchClient mElasticsearchClient;

  public static final Config CONFIG = ConfigFactory.parseFile(new File("conf/application.conf"))
      .resolve();
  private static final String ES_INDEX = CONFIG.getString("es.index.name");
  private static final String ES_TYPE = CONFIG.getString("es.index.type");
  private static final String ES_CLUSTER = CONFIG.getString("es.cluster.name");

  private static final Builder CLIENT_SETTINGS = ImmutableSettings.settingsBuilder()
      .put("index.name", ES_INDEX).put("index.type", ES_TYPE).put("cluster.name", ES_CLUSTER);

  @BeforeClass
  public static void setup() throws IOException {
    mNode = nodeBuilder().settings(CLIENT_SETTINGS).local(true).node();
    mClient = mNode.client();
    mElasticsearchClient = new ElasticsearchClient(mClient);
  }

  @Test
  public void testOnMap() {
    final UUID uuid = UUID.randomUUID();
    mElasticsearchClient.addMap(ElasticsearchDemoData.JSON_MAP, uuid);
    final Map<String, Object> mapGotBack = mElasticsearchClient.getDocument(ES_TYPE, uuid);
    Assert.assertEquals(ElasticsearchDemoData.JSON_MAP, mapGotBack);
  }

  @AfterClass
  public static void closeElasticsearch() {
    mClient.close();
    mNode.close();
  }
}
