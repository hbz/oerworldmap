package services;

import java.io.File;
import java.io.IOException;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import helpers.ElasticsearchHelpers;
import helpers.UniversalFunctions;
import models.Resource;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import services.repository.BaseRepository;

public class BaseRepositoryTest {

  private static BaseRepository mRepo;
  private static Settings mClientSettings;
  private static TransportClient mClient;
  private static ElasticsearchProvider mElClient;
  private static ElasticsearchConfig mEsConfig;
  private static Config mConfig;

  @SuppressWarnings("resource")
  @BeforeClass
  public static void setup() throws IOException {
    mConfig = ConfigFactory.parseFile(new File("conf/test.conf")).resolve();
    mEsConfig = new ElasticsearchConfig(mConfig);
    mClientSettings = ImmutableSettings.settingsBuilder().put(mEsConfig.getClientSettings())
        .build();
    mClient = new TransportClient(mClientSettings)
        .addTransportAddress(new InetSocketTransportAddress(mEsConfig.getServer(), Integer
            .valueOf(mEsConfig.getJavaPort())));
    mElClient = new ElasticsearchProvider(mClient, mEsConfig);
    mElClient.createIndex(mConfig.getString("es.index.name"));
    mRepo = new BaseRepository(mConfig);
  }

  @Test
  public void testResourceWithIdentifiedSubObject() {
    Resource resource = new Resource("Person", "id001");
    String property = "attended";
    Resource value = new Resource("Event", "OER15");
    resource.put(property, value);
    try {
      mRepo.addResource(resource);
    } catch (IOException e) {
      e.printStackTrace();
    }
    Assert.assertEquals(resource, mRepo.getResource("id001"));
    Assert.assertEquals(value, mRepo.getResource("OER15"));
  }

  @Test
  public void testResourceWithUnidentifiedSubObject() {
    Resource resource = new Resource("Person", "id002");
    String property = "attended";
    Resource value = new Resource("Foo", "Foo15");
    resource.put(property, value);
    try {
      mRepo.addResource(resource);
    } catch (IOException e) {
      e.printStackTrace();
    }
    Assert.assertEquals(resource, mRepo.getResource("id002"));
    Assert.assertNull(mRepo.getResource("Foo15"));
  }

  @AfterClass
  public static void tearDown() {
    mElClient.deleteIndex(mConfig.getString("es.index.name"));
  }

}
