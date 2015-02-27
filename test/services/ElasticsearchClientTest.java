package services;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.json.simple.parser.ParseException;
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
    final Map<String, Object> mapGotBack = mElasticsearchClient.getDocument(esConfig.getType(),
        uuid);
    Assert.assertEquals(ElasticsearchDemoData.JSON_MAP, mapGotBack);
  }

  @Test
  public void testEsSearch() throws ParseException {
    final String aQueryString = "_search?*:*";
    try {
      // TODO : this test currently presumes that there is some data existent in your elasticsearch
      // instance. Otherwise it will fail. This restriction can be overturned when a parallel method
      // for the use of POST is introduced in ElasticsearchClient.
      List<Map<String, Object>> result1 = mElasticsearchClient.esQuery(aQueryString);
      List<Map<String, Object>> result2 = mElasticsearchClient.esQuery(aQueryString, "_all");
      List<Map<String, Object>> result3 = mElasticsearchClient.esQuery(aQueryString, "_all", "");
      Assert.assertTrue(!result1.isEmpty());
      Assert.assertTrue(!result2.isEmpty());
      Assert.assertTrue(!result3.isEmpty());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @AfterClass
  public static void closeElasticsearch() {
    mClient.close();
    mNode.close();
  }
}
