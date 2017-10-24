package helpers;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.junit.AfterClass;
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
  protected static Types mTypes;

  public static ElasticsearchRepository getEsRepo() {
    return mRepo;
  }

  @BeforeClass
  public static void setup() throws IOException {
    mConfig = ConfigFactory.parseFile(new File("conf/test.conf")).resolve();
    try {
      mTypes = new Types(mConfig);
    } catch (ProcessingException e) {
      e.printStackTrace();
    }
    mRepo = new ElasticsearchRepository(mConfig, mTypes);
    mEsConfig = mRepo.getConfig();

    mClientSettings = Settings.settingsBuilder().put(mEsConfig.getClientSettings())
      .build();
    mClient = TransportClient.builder().settings(mClientSettings).build()
      .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(mEsConfig.getServer()),
        Integer.valueOf(mEsConfig.getJavaPort())));
  }

  @AfterClass
  public static void tearDown() throws Exception {
    if (mConfig.getBoolean("es.node.inmemory")) {
      mEsConfig.deleteIndices(mEsConfig.getAllIndices());
    }
    mEsConfig.tearDown();
  }

}
