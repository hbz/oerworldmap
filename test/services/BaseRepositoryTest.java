package services;

import java.io.IOException;
import java.nio.file.Paths;

import models.Resource;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import controllers.Global;
import services.repository.BaseRepository;

public class BaseRepositoryTest {

  private static BaseRepository mRepo;
  private static Settings mClientSettings;
  private static TransportClient mClient;
  private static ElasticsearchProvider mElClient;
  private static final ElasticsearchConfig mEsConfig = Global.getElasticsearchConfig();

  @SuppressWarnings("resource")
  @BeforeClass
  public static void setup() throws IOException {
    mClientSettings = ImmutableSettings.settingsBuilder().put(mEsConfig.getClientSettings())
        .build();
    mClient = new TransportClient(mClientSettings)
        .addTransportAddress(new InetSocketTransportAddress(mEsConfig.getServer(), Integer
            .valueOf(mEsConfig.getJavaPort())));
    mElClient = new ElasticsearchProvider(mClient, mEsConfig);
    mRepo = new BaseRepository(Global.getConfig());
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
}
