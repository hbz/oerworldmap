package services;

import com.typesafe.config.Config;
import helpers.UniversalFunctions;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.common.xcontent.XContentType;
import play.Logger;

import java.io.IOException;
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
  private RestHighLevelClient mEsClient;
  private WriteRequest.RefreshPolicy mRefreshPolicy;


  public WriteRequest.RefreshPolicy getRefreshPolicy() {
    return mRefreshPolicy;
  }

  public ElasticsearchConfig(Config aConfiguration) {
    mConfig = aConfiguration;

    // HOST
    mServer = mConfig.getString("es.host.server");
    mJavaPort = mConfig.getInt("es.host.port.http");

    mIndex = mConfig.getString("es.index.name");
    mType = mConfig.getString("es.index.type");
    mCluster = mConfig.getString("es.cluster.name");

    final RestClientBuilder builder = RestClient.builder(
      new HttpHost(mServer, mJavaPort, "http"));
    mEsClient = new RestHighLevelClient(builder);

    if (!indexExists(mIndex)) {
      CreateIndexResponse response = null;
      try {
        response = createIndex(mIndex);
      } catch (IOException e) {
        Logger.error("Failing to create index '".concat(mIndex).concat("', caused by: "), e);
      }
      if (response.isAcknowledged()) {
        Logger.info("Created index \"" + mIndex + "\".");
      }
    }

    // INDEX SETTINGS
    mIndexSettings = new HashMap<>();
    mIndexSettings.put("index.name", mIndex);
    mIndexSettings.put("index.type", mType);

    // CLUSTER SETTINGS
    mClusterSettings = new HashMap<>();
    mClusterSettings.put("cluster.name", mCluster);

    switch (mConfig.getString("es.request.refreshpolicy")) {
      case ("IMMEDIATE"):
        mRefreshPolicy = WriteRequest.RefreshPolicy.IMMEDIATE;
        break;
      case ("WAIT_UNTIL"):
        mRefreshPolicy = WriteRequest.RefreshPolicy.WAIT_UNTIL;
        break;
      default:
        mRefreshPolicy = WriteRequest.RefreshPolicy.NONE;
    }
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

  public String toString() {
    return mConfig.toString();
  }

  public String getIndexConfigString() throws IOException {
    return UniversalFunctions.readFile(INDEX_CONFIG_FILE, StandardCharsets.UTF_8);
  }

  public Map<String, String> getClusterSettings() {
    return mClusterSettings;
  }

  public RestHighLevelClient getClient() {
    return mEsClient;
  }

  public boolean indexExists(String aIndex) {
    GetIndexRequest request = new GetIndexRequest().indices(aIndex);
    // TODO with ES v 6.3: return mEsClient.indices().exists(request);
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpHead head = new HttpHead(
      "http://".concat(mServer).concat(":").concat(mJavaPort.toString()).concat("/")
        .concat(aIndex));
    try {
      final CloseableHttpResponse response = httpClient.execute(head);
      return (response.getStatusLine().getStatusCode() == 200);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }

  public CreateIndexResponse createIndex(String aIndex) throws IOException {
    CreateIndexRequest request = new CreateIndexRequest(aIndex);
    // TODO : to be configured (optional)
    /*request.settings(Settings.builder()
      .put("index.number_of_shards", 3)
      .put("index.number_of_replicas", 2)
    );*/
    request.source(getIndexConfigString(), XContentType.JSON);
    return mEsClient.indices().create(request);
  }

  public DeleteIndexResponse deleteIndex(String aIndex) throws IOException {
    DeleteIndexRequest request = new DeleteIndexRequest(aIndex);
    return mEsClient.indices().delete(request);
  }

  public void tearDown() throws Exception {
    if (mEsClient != null) {
      mEsClient.close();
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
