package helpers;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import play.test.WithApplication;
import services.ElasticsearchConfig;
import services.repository.ElasticsearchRepository;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

public class ElasticsearchTestGrid extends WithApplication {

  protected static Config mConfig;
  protected static ElasticsearchRepository mRepo;
  protected static Settings mClientSettings;
  protected static Client mClient;
  protected static ElasticsearchConfig mEsConfig;

  @BeforeClass
  public static void setup() throws IOException {
    mConfig = ConfigFactory.parseFile(new File("conf/test.conf")).resolve();
    mEsConfig = new ElasticsearchConfig(mConfig);
    mRepo = new ElasticsearchRepository(mConfig);

    mClientSettings = Settings.settingsBuilder().put(mEsConfig.getClientSettings())
      .build();
    mClient = TransportClient.builder().settings(mClientSettings).build()
      .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(mEsConfig.getServer()),
        Integer.valueOf(mEsConfig.getJavaPort())));
  }

  @AfterClass
  public static void tearDown() throws Exception {
    if (mConfig.getBoolean("es.node.inmemory")) {
      mEsConfig.deleteIndex(mConfig.getString("es.index.name"));
    }
    mEsConfig.tearDown();
  }

  @Before
  public void setupIndex() {
    ElasticsearchHelpers.cleanIndex(mRepo, mConfig.getString("es.index.name"));
  }

}
