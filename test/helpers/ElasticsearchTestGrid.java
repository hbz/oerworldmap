package helpers;

import java.io.File;
import java.io.IOException;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import services.ElasticsearchConfig;
import services.ElasticsearchProvider;
import services.repository.BaseRepository;
import services.repository.ElasticsearchRepository;

public class ElasticsearchTestGrid {
  protected static Config mConfig;
  protected static ElasticsearchRepository mRepo;

  protected static BaseRepository mBaseRepo;
  protected static Settings mClientSettings;
  protected static Client mClient;
  protected static ElasticsearchProvider mEsProvider;
  protected static ElasticsearchConfig mEsConfig;

  @SuppressWarnings("resource")
  @BeforeClass
  public static void setup() throws IOException {

    mConfig = ConfigFactory.parseFile(new File("conf/test.conf")).resolve();
    mEsConfig = new ElasticsearchConfig(mConfig);

    mBaseRepo = new BaseRepository(mConfig);
    mRepo = new ElasticsearchRepository(mConfig);

    mClientSettings = ImmutableSettings.settingsBuilder().put(mEsConfig.getClientSettings())
        .build();
    mClient = new TransportClient(mClientSettings)
        .addTransportAddress(new InetSocketTransportAddress(mEsConfig.getServer(),
            Integer.valueOf(mEsConfig.getJavaPort())));
    mEsProvider = new ElasticsearchProvider(mClient, mEsConfig);

    ElasticsearchHelpers.cleanIndex(mRepo.getElasticsearchProvider(),
        mConfig.getString("es.index.name"));

  }

  @AfterClass
  public static void tearDown() {
    mEsProvider.deleteIndex(mConfig.getString("es.index.name"));
    mClient.close();
  }
}
