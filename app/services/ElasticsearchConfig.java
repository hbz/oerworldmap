package services;

import helpers.UniversalFunctions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

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
  private String mAppIndex;
  private String mTestIndex;
  private String mType;
  private String mCluster;
  private Map<String, String> mClientSettings;
  private Builder mClientSettingsBuilder;

  public ElasticsearchConfig(boolean isTest) {
    this(null, isTest);
  }

  public ElasticsearchConfig(String aFilename, boolean isTest) {
    File configFile;
    if (!StringUtils.isEmpty(aFilename)) {
      configFile = new File(aFilename);
    } else {
      configFile = new File(DEFAULT_CONFIG_FILE);
    }
    init(configFile, isTest);
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

  private void init(File aConfigFile, boolean isTest) {
    checkFileExists(aConfigFile);

    // CONFIG FILE
    mConfig = ConfigFactory.parseFile(aConfigFile).resolve();

    // HOST
    mServer = mConfig.getString("es.host.server");
    mJavaPort = mConfig.getString("es.host.port.java");
    mHttpPort = mConfig.getString("es.host.port.http");
    mNode = new InetSocketTransportAddress(mServer, Integer.valueOf(mJavaPort));

    // CLIENT
    mAppIndex = mConfig.getString("es.index.app.name");
    mTestIndex = mConfig.getString("es.index.test.name");
    mType = mConfig.getString("es.index.type");
    mCluster = mConfig.getString("es.cluster.name");

    mClientSettings = new HashMap<String, String>();
    if (isTest) {
      mClientSettings.put("index.name", mTestIndex);
    } else {
      mClientSettings.put("index.name", mAppIndex);
    }
    mClientSettings.put("index.type", mType);
    mClientSettings.put("cluster.name", mCluster);

    mClientSettingsBuilder = ImmutableSettings.settingsBuilder().put(mClientSettings);
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
}
