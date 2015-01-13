package utilities;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class ElasticsearchClient {

  public static final Config CONFIG = ConfigFactory.parseFile(new File("conf/application.conf"))
      .resolve();

  private static final String ES_SERVER = CONFIG.getString("es.host.server");
  private static final Integer ES_PORT = CONFIG.getInt("es.host.port");
  private static final String ES_INDEX = CONFIG.getString("es.index.name");
  private static final String ES_TYPE = CONFIG.getString("es.index.type");
  private static final String ES_CLUSTER = CONFIG.getString("es.cluster.name");

  private static final InetSocketTransportAddress ES_NODE = new InetSocketTransportAddress(
      ES_SERVER, ES_PORT);

  private static final Builder CLIENT_SETTINGS = ImmutableSettings.settingsBuilder()
      .put("index.name", ES_INDEX)
      .put("index.type", ES_TYPE)
      .put("cluster.name", ES_CLUSTER);

  private static Client mClient;

  public ElasticsearchClient() {
    TransportClient tc = new TransportClient(CLIENT_SETTINGS
        .put("client.transport.ping_timeout",20, TimeUnit.SECONDS)
        .build());
    mClient = tc.addTransportAddress(ES_NODE);
    mClient.admin().indices().prepareUpdateSettings(ES_INDEX)
        .setSettings(ImmutableMap.of("index.refresh_interval", "1"))
        .execute().actionGet();
  }
  
  public Client getClient() {
    return mClient;
  }

  public static String getIndex() {
    return ES_INDEX;
  }

  public static String getType() {
    return ES_TYPE;
  }
}
