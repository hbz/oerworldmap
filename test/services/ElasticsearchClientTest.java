package services;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ElasticsearchClientTest {
  protected static Client mClient;
  protected static Node mNode;
  protected static ElasticsearchClient mElasticsearchClient;

  private static final ElasticsearchConfig esConfig = new ElasticsearchConfig();
  
  @BeforeClass
  public static void setup() throws IOException {
    mNode = nodeBuilder().settings(esConfig.getClientSettingsBuilder()).local(true).node();
    mClient = mNode.client();
    mElasticsearchClient = new ElasticsearchClient(mClient);
  }

  @Test
  public void testOnMap() {
    final UUID uuid = UUID.randomUUID();
    mElasticsearchClient.addMap(ElasticsearchDemoData.JSON_MAP, uuid);
    final Map<String, Object> mapGotBack = mElasticsearchClient.getDocument(esConfig.getType(), uuid);
    Assert.assertEquals(ElasticsearchDemoData.JSON_MAP, mapGotBack);
  }

  @AfterClass
  public static void closeElasticsearch() {
    mClient.close();
    mNode.close();
  }
}
