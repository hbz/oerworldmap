package services;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.elasticsearch.action.search.SearchResponse;
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

public class ElasticsearchProviderTest {
  protected static Client mClient;
  protected static ElasticsearchProvider mElasticsearchProvider;
  private static Config mConfig;
  private static ElasticsearchConfig mEsConfig;

  @SuppressWarnings("resource")
  @BeforeClass
  public static void setup() throws IOException {
    mConfig = ConfigFactory.parseFile(new File("conf/test.conf")).resolve();
    mEsConfig = new ElasticsearchConfig(mConfig);
    final Settings mClientSettings = ImmutableSettings.settingsBuilder()
          .put(mEsConfig.getClientSettings()).build();
    mClient = new TransportClient(mClientSettings)
          .addTransportAddress(new InetSocketTransportAddress(mEsConfig.getServer(),
              9300));
    mElasticsearchProvider = new ElasticsearchProvider(mClient, mEsConfig);
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
      SearchResponse result1 = mElasticsearchProvider.esQuery(aQueryString);
      SearchResponse result2 = mElasticsearchProvider.esQuery(aQueryString, "_all", null);
      SearchResponse result3 = mElasticsearchProvider.esQuery(aQueryString, "_all", "");
      Assert.assertTrue(result1.getHits().getTotalHits() > 1);
      Assert.assertTrue(result2.getHits().getTotalHits() > 1);
      Assert.assertTrue(result3.getHits().getTotalHits() > 1);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @AfterClass
  public static void closeElasticsearch() {
    mClient.close();
  }
}
