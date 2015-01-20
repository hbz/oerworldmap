import java.io.File;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import play.Application;
import play.GlobalSettings;
import play.Logger;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Global extends GlobalSettings {

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
      .put("index.name", ES_INDEX).put("index.type", ES_TYPE).put("cluster.name", ES_CLUSTER)
      .put("client.transport.ping_timeout", 20, TimeUnit.SECONDS);

  private Client mClient;

  @Override
  public void onStart(Application app) {
    Logger.info("oerworldmap has started");
    Logger.info("Elasticsearch config: " + CONFIG);
    setupElClient();
    setupElIndex();
  }

  @Override
  public void onStop(Application app) {
    Logger.info("oerworldmap shutdown...");
  }

  @SuppressWarnings("resource")
  public void setupElClient() {
    final TransportClient tc = new TransportClient(CLIENT_SETTINGS.build());
    mClient = tc.addTransportAddress(ES_NODE);
    mClient.admin().indices().prepareUpdateSettings(ES_INDEX)
        .setSettings(ImmutableMap.of("index.refresh_interval", "1")).execute().actionGet();
  }

  private void setupElIndex() {
    if (mClient == null) {
      throw new java.lang.IllegalStateException(
          "Trying to set Elasticsearch index with no existing client.");
    }
    if (!indexExists(mClient, ES_INDEX)) {
      mClient.admin().indices().create(new CreateIndexRequest(ES_INDEX)).actionGet();
    }
    mClient.admin().indices().refresh(new RefreshRequest(ES_INDEX)).actionGet();
  }
  
  // return true if the specified Index already exists on the specified Client. 
  public boolean indexExists(Client aClient, String aIndex){
    return aClient.admin().indices().prepareExists(aIndex).execute().actionGet().isExists();
  }

}
