package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.typesafe.config.Config;
import helpers.Types;
import helpers.UniversalFunctions;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import play.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ElasticsearchConfig {

  // CONFIG FILE
  private Config mConfig;

  // HOST
  private String mServer;
  private Integer mJavaPort;
  private Node mInternalNode;

  // CLIENT
  private String mWebpageIndex;
  private String mWebpageMapping;
  private String mActionIndex;
  private String mActionType;
  private String mActionMapping;
  private String mCluster;
  private Map<String, String> mClientSettings;
  private Client mClient;
  private TransportClient mTransportClient;

  private String[] mAllIndices;
  final private Types mTypes;

  final private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public ElasticsearchConfig(final Config aConfiguration, final Types aTypes) throws IOException {
    mTypes = aTypes;
    mConfig = aConfiguration;
    init();
  }

  private void init() throws IOException {
    // HOST
    mServer = mConfig.getString("es.host.server");
    mJavaPort = mConfig.getInt("es.host.port.java");

    mWebpageIndex = mConfig.getString("es.index.webpage.name");
    mWebpageMapping = mConfig.getString("es.index.webpage.mapping.file");
    mActionIndex = mConfig.getString("es.index.action.name");
    mActionType = mConfig.getString("es.index.action.type");
    mActionMapping = mConfig.getString("es.index.action.mapping.file");

    mCluster = mConfig.getString("es.cluster.name");

    mAllIndices = new String[]{mWebpageIndex, mActionIndex};

    if (mConfig.getBoolean("es.node.inmemory") || (mJavaPort == null) || Strings.isNullOrEmpty(mServer)) {
      mInternalNode = NodeBuilder.nodeBuilder().local(true).data(true).node();
      mClient = mInternalNode.client();
    } //
    else {
      Settings clientSettings = Settings.settingsBuilder() //
          .put("cluster.name", mCluster) //
          .put("client.transport.sniff", true) //
          .build();
      mTransportClient = TransportClient.builder().settings(clientSettings).build();
      try {
        mClient = mTransportClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(mServer),
          mJavaPort));
      } catch (UnknownHostException ex) {
        throw new RuntimeException(ex);
      }
    }

    createAllIndices();

    // CLIENT SETTINGS
    mClientSettings = new HashMap<>();
    mClientSettings.put("index.webpage.name", mWebpageIndex);
    mClientSettings.put("index.action.name", mActionIndex);
    mClientSettings.put("index.action.type", mActionType);
    mClientSettings.put("cluster.name", mCluster);
  }

  public String[] getAllIndices(){
    return mAllIndices;
  }

  public String getIndex(final Class aClass){
    return mTypes.getEsIndexFromClassType(aClass);
  }

  public String getServer() {
    return mServer;
  }

  public Integer getJavaPort() {
    return mJavaPort;
  }

  public Map<String, String> getClientSettings() {
    return mClientSettings;
  }

  private boolean indexExists(String aIndex) {
    return mClient.admin().indices().prepareExists(aIndex).execute().actionGet().isExists();
  }

  public void createAllIndices() throws IOException {
    createIndexIfNotExists(mWebpageIndex, mWebpageMapping);
    createIndexIfNotExists(mActionIndex, mActionMapping);
  }

  public void createIndexIfNotExists(final String aIndex, final String aIndexConfig) throws IOException {
    if (!indexExists(aIndex)) {
      final String indexConfigString = UniversalFunctions.readFile(aIndexConfig, StandardCharsets.UTF_8);
      JsonNode indexConfigNode = OBJECT_MAPPER.readTree(indexConfigString);
      CreateIndexResponse response = mClient.admin().indices().prepareCreate(aIndex)
        .addMapping("WebPage", indexConfigNode.get("mappings").get("WebPage").toString())
        .setSettings(indexConfigNode.get("settings").toString())
        .execute().actionGet();
      refreshElasticsearch(aIndex);
      if (response.isAcknowledged()) {
        Logger.info("Created index \"" + aIndex + "\".");
      } //
      else {
        Logger.warn("Not able to create index \"" + aIndex + "\".");
      }
    }
  }

  public DeleteIndexResponse deleteIndices(String... aIndices) {
    return mClient.admin().indices().delete(new DeleteIndexRequest(aIndices)).actionGet();
  }

  private void refreshElasticsearch(String... aIndices) {
    mClient.admin().indices().refresh(new RefreshRequest(aIndices)).actionGet();
  }

  public void tearDown() throws Exception {
    if (mClient != null) {
      mClient.close();
    }
    if (mTransportClient != null) {
      mTransportClient.close();
    }
    if (mInternalNode != null){
      mInternalNode.close();
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
