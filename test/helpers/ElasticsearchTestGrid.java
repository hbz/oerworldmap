package helpers;

import java.io.File;
import java.io.IOException;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import services.ElasticsearchConfig;

public class ElasticsearchTestGrid {

  protected static Config mConfig;
  protected static Settings mClientSettings;
  protected static Client mClient;
  protected static ElasticsearchConfig mEsConfig;

  @BeforeClass
  public static void setup() throws IOException {

    mConfig = ConfigFactory.parseFile(new File("conf/test.conf")).resolve();
    mEsConfig = new ElasticsearchConfig(mConfig);

    mClientSettings = mEsConfig.getClientSettingsBuilder().build();
    mClient = mEsConfig.getClient();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    if (mConfig.getBoolean("es.node.inmemory")) {
      mEsConfig.deleteIndex(mConfig.getString("es.index.name"));
    }
    mEsConfig.tearDown();
  }
}
