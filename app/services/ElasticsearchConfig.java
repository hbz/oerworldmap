package services;

import com.typesafe.config.Config;
import helpers.UniversalFunctions;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import play.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ElasticsearchConfig {

  private static final String INDEX_CONFIG_FILE = "conf/index-config.json";

  // CONFIG FILE
  private Config mConfig;

  // HOST
  private String mServer;
  private Integer mJavaPort;

  // CLIENT
  private String mIndex;
  private String mType;
  private String mCluster;
  private Map<String, String> mIndexSettings;
  private Map<String, String> mClusterSettings;
  private Client mClient;
  private TransportClient mTransportClient;

  public ElasticsearchConfig(Config aConfiguration) {
    mConfig = aConfiguration;

    // HOST
    mServer = mConfig.getString("es.host.server");
    mJavaPort = mConfig.getInt("es.host.port.java");

    mIndex = mConfig.getString("es.index.name");
    mType = mConfig.getString("es.index.type");
    mCluster = mConfig.getString("es.cluster.name");

    Settings clientSettings = Settings.builder() //
        .put("cluster.name", mCluster) //
        .put("client.transport.sniff", true) //
        .build();
    mTransportClient = new PreBuiltTransportClient(clientSettings);

    try {
      mClient = mTransportClient.addTransportAddress(new TransportAddress(InetAddress.getByName(mServer),
        mJavaPort));
    } catch (UnknownHostException ex) {
      throw new RuntimeException(ex);
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

    // INDEX SETTINGS
    mIndexSettings = new HashMap<>();
    mIndexSettings.put("index.name", mIndex);
    mIndexSettings.put("index.type", mType);

    // CLUSTER SETTINGS
    mClusterSettings = new HashMap<>();
    mClusterSettings.put("cluster.name", mCluster);
  }

  public String getIndex() {
    return mIndexSettings.get("index.name");
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

  public Integer getJavaPort() {
    return mJavaPort;
  }

  public String toString() {
    return mConfig.toString();
  }

  public String getIndexConfigString() throws IOException {
    return UniversalFunctions.readFile(INDEX_CONFIG_FILE, StandardCharsets.UTF_8);
  }

  public Map<String, String> getClusterSettings() {
    return mClusterSettings;
  }

  public Client getClient() {
    return mClient;
  }

  private boolean indexExists(String aIndex) {
    return mClient.admin().indices().prepareExists(aIndex).execute().actionGet().isExists();
  }

  private CreateIndexResponse createIndex(String aIndex) {
    return mClient.admin().indices().prepareCreate(aIndex).execute().actionGet();
  }

  public DeleteIndexResponse deleteIndex(String aIndex) {
    return mClient.admin().indices().delete(new DeleteIndexRequest(aIndex)).actionGet();
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
  }

  public Fuzziness getFuzziness() {
    String fuzzyString = mConfig.getString("es.search.fuzziness");
    if (StringUtils.isEmpty(fuzzyString) || fuzzyString.equals("AUTO")) {
      return Fuzziness.AUTO;
    }
    if (fuzzyString.equals("ZERO")) {
      return Fuzziness.ZERO;
    }
    if (fuzzyString.equals("ONE")) {
      return Fuzziness.ONE;
    }
    if (fuzzyString.equals("TWO")) {
      return Fuzziness.TWO;
    }
    return Fuzziness.AUTO;
  }

}
