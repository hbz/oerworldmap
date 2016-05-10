package services;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.Fuzziness;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import helpers.UniversalFunctions;

public class ElasticsearchConfig {

  private static final String DEFAULT_CONFIG_FILE = "conf/application.conf";
  private static final String INDEX_CONFIG_FILE = "conf/index-config.json";

  // CONFIG FILE
  private Config mConfig;

  // HOST
  private String mServer;
  private String mJavaPort;
  private String mHttpPort;
  private InetSocketTransportAddress mNode;

  // CLIENT
  private String mIndex;
  private String mType;
  private String mCluster;
  private Map<String, String> mClientSettings;
  private Builder mClientSettingsBuilder;

  public ElasticsearchConfig(String aFilename) {
    File configFile;
    if (!StringUtils.isEmpty(aFilename)) {
      configFile = new File(aFilename);
    } else {
      configFile = new File(DEFAULT_CONFIG_FILE);
    }
    init(configFile);
  }

  public ElasticsearchConfig(Config aConfiguration) {
    init(aConfiguration);
  }

  private void checkFileExists(File file) {
    if (!file.exists()) {
      try {
        throw new java.io.FileNotFoundException("Elasticsearch config file \""
            + file.getAbsolutePath() + "\" not found.");
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    }
  }


  private void init() {
    // HOST
    mServer = mConfig.getString("es.host.server");
    mJavaPort = mConfig.getString("es.host.port.java");
    mHttpPort = mConfig.getString("es.host.port.http");
    mNode = new InetSocketTransportAddress(mServer, Integer.valueOf(mJavaPort));

    // CLIENT
    mIndex = mConfig.getString("es.index.name");
    mType = mConfig.getString("es.index.type");
    mCluster = mConfig.getString("es.cluster.name");

    mClientSettings = new HashMap<String, String>();
    mClientSettings.put("index.name", mIndex);
    mClientSettings.put("index.type", mType);
    mClientSettings.put("cluster.name", mCluster);

    mClientSettingsBuilder = ImmutableSettings.settingsBuilder().put(mClientSettings);
  }

  private void init(File aConfigFile) {
    // CONFIG FILE
    checkFileExists(aConfigFile);
    mConfig = ConfigFactory.parseFile(aConfigFile).resolve();
    init();
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

  public Map<String, String> getClientSettings() {
    return mClientSettings;
  }

  public Builder getClientSettingsBuilder() {
    return mClientSettingsBuilder;
  }

  public String getServer() {
    return mServer;
  }

  public String getJavaPort() {
    return mJavaPort;
  }

  public String getHttpPort() {
    return mHttpPort;
  }

  public InetSocketTransportAddress getNode() {
    return mNode;
  }

  public String toString() {
    return mConfig.toString();
  }

  public String getIndexConfigString() throws IOException {
    return UniversalFunctions.readFile(INDEX_CONFIG_FILE, StandardCharsets.UTF_8);
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
