package services;

import java.io.File;
import java.io.IOException;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import helpers.ElasticsearchHelpers;
import helpers.JsonTest;
import models.Resource;
import services.repository.BaseRepository;

public class BaseRepositoryTest implements JsonTest {

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
        .addTransportAddress(new InetSocketTransportAddress(mEsConfig.getServer(),
            Integer.valueOf(mEsConfig.getJavaPort())));
    mElClient = new ElasticsearchProvider(mClient, mEsConfig);
    mElClient.createIndex(mConfig.getString("es.index.name"));
    mRepo = new BaseRepository(mConfig);
  }

  @Test
  public void testResourceWithIdentifiedSubObject() throws IOException {
    Resource resource = new Resource("Person", "id001");
    String property = "attended";
    Resource value = new Resource("Event", "OER15");
    resource.put(property, value);
    Resource expected1 = getResourceFromJsonFile(
        "BaseRepositoryTest/testResourceWithIdentifiedSubObject.OUT.1.json");
    Resource expected2 = getResourceFromJsonFile(
        "BaseRepositoryTest/testResourceWithIdentifiedSubObject.OUT.2.json");
    mRepo.addResource(resource);
    Assert.assertEquals(expected1, mRepo.getResource("id001"));
    Assert.assertEquals(expected2, mRepo.getResource("OER15"));
  }

  @Test
  public void testResourceWithUnidentifiedSubObject() throws IOException {
    Resource resource = new Resource("Person", "id002");
    Resource value = new Resource("Foo", null);
    resource.put("attended", value);
    Resource expected = getResourceFromJsonFile(
        "BaseRepositoryTest/testResourceWithUnidentifiedSubObject.OUT.1.json");
    mRepo.addResource(resource);
    Assert.assertEquals(expected, mRepo.getResource("id002"));
  }

  @AfterClass
  public static void clean() throws IOException {
    ElasticsearchHelpers.cleanIndex(mElClient, mEsConfig.getIndex());
  }

  @AfterClass
  public static void tearDown() {
    mElClient.deleteIndex(mConfig.getString("es.index.name"));
  }

}
