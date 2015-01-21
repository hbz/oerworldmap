import java.util.concurrent.TimeUnit;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;

import play.Application;
import play.GlobalSettings;
import play.Logger;
import services.ElasticsearchConfig;

public class Global extends GlobalSettings {

  private static ElasticsearchConfig esConfig = new ElasticsearchConfig();

  private static final Builder CLIENT_SETTINGS = esConfig.getClientSettings()
      .put("client.transport.ping_timeout", 20, TimeUnit.SECONDS);

  private Client mClient;

  @Override
  public void onStart(Application app) {
    Logger.info("oerworldmap has started");
    Logger.info("Elasticsearch config: " + esConfig.toString());
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
    mClient = tc.addTransportAddress(esConfig.getNode());
    mClient.admin().indices().prepareUpdateSettings(esConfig.getIndex())
        .setSettings(ImmutableMap.of("index.refresh_interval", "1")).execute().actionGet();
  }

  private void setupElIndex() {
    if (mClient == null) {
      throw new java.lang.IllegalStateException(
          "Trying to set Elasticsearch index with no existing client.");
    }
    if (!indexExists(mClient, esConfig.getIndex())) {
      mClient.admin().indices().create(new CreateIndexRequest(esConfig.getIndex())).actionGet();
    }
    mClient.admin().indices().refresh(new RefreshRequest(esConfig.getIndex())).actionGet();
  }

  // return true if the specified Index already exists on the specified Client.
  public boolean indexExists(Client aClient, String aIndex) {
    return aClient.admin().indices().prepareExists(aIndex).execute().actionGet().isExists();
  }

}
