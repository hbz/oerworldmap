package services;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import controllers.Global;

public class ElasticsearchProviderTest {
  protected static Client mClient;
  protected static ElasticsearchProvider mElasticsearchProvider;

  private static final ElasticsearchConfig mEsConfig = Global.getElasticsearchConfig();

  @SuppressWarnings("resource")
  @BeforeClass
  public static void setup() throws IOException {
    final Settings mClientSettings = ImmutableSettings.settingsBuilder()
          .put(mEsConfig.getClientSettings()).build();
    mClient = new TransportClient(mClientSettings)
          .addTransportAddress(new InetSocketTransportAddress(mEsConfig.getServer(),
              9300));
    mElasticsearchProvider = new ElasticsearchProvider(mClient, mEsConfig, true);
  }

  @Test
  public void testOnMap() {
    final UUID uuid = UUID.randomUUID();
    mElasticsearchProvider.addMap(ElasticsearchDemoData.JSON_MAP, uuid);
    final Map<String, Object> mapGotBack = mElasticsearchProvider.getDocument(mEsConfig.getType(),
        uuid);
    Assert.assertEquals(ElasticsearchDemoData.JSON_MAP, mapGotBack);
  }

  //@Test
  public void testEsSearch() throws ParseException {
    final String aQueryString = "*";
    try {
      // TODO : this test currently presumes that there is some data existent in your elasticsearch
      // instance. Otherwise it will fail. This restriction can be overturned when a parallel method
      // for the use of POST is introduced in ElasticsearchProvider.
      List<Map<String, Object>> result1 = mElasticsearchProvider.esQuery(aQueryString);
      List<Map<String, Object>> result2 = mElasticsearchProvider.esQuery(aQueryString, "_all", null);
      List<Map<String, Object>> result3 = mElasticsearchProvider.esQuery(aQueryString, "_all", "");
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
  }
}
