package services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import com.typesafe.config.Config;

import helpers.UniversalFunctions;
import play.Logger;

public class ElasticsearchConfig {

  private static final String INDEX_CONFIG_FILE = "conf/index-config.json";

  // CONFIG FILE
  private Config mConfig;

  // HOST
  private String mServer;
  private String mJavaPort;
  private Node mInternalNode;

  // CLIENT
  private String mIndex;
  private String mType;
  private String mCluster;
  private Map<String, String> mClientSettings;
  private Builder mClientSettingsBuilder;
  private Client mClient;
  private TransportClient mTransportClient;

  public ElasticsearchConfig(Config aConfiguration) {
    init(aConfiguration);
  }

  private void init() {

    // HOST
    mServer = mConfig.getString("es.host.server");
    mJavaPort = mConfig.getString("es.host.port.java");

    mIndex = mConfig.getString("es.index.name");
    mType = mConfig.getString("es.index.type");
    mCluster = mConfig.getString("es.cluster.name");

    if (mConfig.getBoolean("es.node.inmemory")) {
      mInternalNode = NodeBuilder.nodeBuilder().local(true).data(true).node();
      mClient = mInternalNode.client();
    } //
    else {
      Settings clientSettings = ImmutableSettings.settingsBuilder() //
          .put("cluster.name", mCluster) //
          .put("client.transport.sniff", true) //
          .build();
      mTransportClient = new TransportClient(clientSettings);
      mClient = mTransportClient.addTransportAddress(new InetSocketTransportAddress(mServer, 9300));
    }

    if (!indexExists(mIndex)) {
      CreateIndexResponse response = createIndex(mIndex);
      refreshElasticsearch(mIndex);
      if (response.isAcknowledged()) {
        Logger.info("Created index \"" + mIndex + "\".");
      } //
      else {
        Logger.warn("Not able to create index \"" + mIndex + "\".");
      }
    }

    // CLIENT SETTINGS
    mClientSettings = new HashMap<String, String>();
    mClientSettings.put("index.name", mIndex);
    mClientSettings.put("index.type", mType);
    mClientSettings.put("cluster.name", mCluster);

    mClientSettingsBuilder = ImmutableSettings.settingsBuilder().put(mClientSettings);
  }

  private void init(Config aConfiguration) {
    // CONFIG OBJECT
    mConfig = aConfiguration;
    init();
  }


  public String getIndex() {
    return mClientSettings.get("index.name");
  }

  public String getType() {
    return mType;
  }

  public String getCluster() {
    return mCluster;
  }

  public String getServer() {
    return mServer;
  }

  public String getJavaPort() {
    return mJavaPort;
  }

  public String toString() {
    return mConfig.toString();
  }

  public String getIndexConfigString() throws IOException {
    return UniversalFunctions.readFile(INDEX_CONFIG_FILE, StandardCharsets.UTF_8);
  }

  public Builder getClientSettingsBuilder() {
    return mClientSettingsBuilder;
  }

  public Client getClient() {
    return mClient;
  }

  private boolean indexExists(String aIndex) {
    return mClient.admin().indices().prepareExists(aIndex).execute().actionGet().isExists();
  }

  private CreateIndexResponse createIndex(String aIndex) {
    CreateIndexResponse response = mClient.admin().indices().prepareCreate(mIndex).execute().actionGet();
    // mClient.admin().cluster().health(new
    // ClusterHealthRequest(aIndex).waitForActiveShards(1)).actionGet();
    mClient.admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet(5000);
    return response;
  }

  private void refreshElasticsearch(String aIndex) {
    mClient.admin().indices().refresh(new RefreshRequest(aIndex)).actionGet();
  }

  public void tearDown() throws Exception {
    if (mClient != null) {
      mClient.close();
    }
    if (mTransportClient != null) {
      mTransportClient.close();
    }
    if (mInternalNode != null){
      mInternalNode.stop();
      mInternalNode.close();
    }
  }

}
