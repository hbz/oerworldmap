package services;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class ElasticsearchConfig {

  private static final String DEFAULT_CONFIG_FILE = "conf/application.conf";

  // CONFIG FILE
  private Config mConfig;

  // HOST
  private String mServer;
  private String mPort;
  private InetSocketTransportAddress mNode;

  // CLIENT
  private String mIndex;
  private String mType;
  private String mCluster;
  private Map<String, String> mClientSettings;
  private Builder mClientSettingsBuilder;

  public ElasticsearchConfig() {
    File configFile = new File(DEFAULT_CONFIG_FILE);
    init(configFile);
  }

  public ElasticsearchConfig(String aFilename) {
    File file = new File(aFilename);
    init(file);
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

  private void init(File aConfigFile) {
    checkFileExists(aConfigFile);

    // CONFIG FILE
    mConfig = ConfigFactory.parseFile(aConfigFile).resolve();

    // HOST
    mServer = mConfig.getString("es.host.server");
    mPort = mConfig.getString("es.host.port");
    mNode = new InetSocketTransportAddress(mServer, Integer.valueOf(mPort));

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

  public String getIndex() {
    return mIndex;
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

  public String getPort() {
    return mPort;
  }

  public InetSocketTransportAddress getNode() {
    return mNode;
  }

  public String toString() {
    return mConfig.toString();
  }
}
