package services;

import helpers.ElasticsearchHelpers;
import helpers.JsonTest;

import java.io.IOException;
import java.nio.file.Paths;

import models.Resource;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import controllers.Global;

public class BaseRepositoryTest implements JsonTest{

  private static BaseRepository mRepo;
  private static Settings mClientSettings;
  private static TransportClient mClient;
  private static ElasticsearchConfig mEsConfig = Global.getElasticsearchConfig();
  private static ElasticsearchProvider mElClient;

  @SuppressWarnings("resource")
  @BeforeClass
  public static void setup() throws IOException {
    if (mEsConfig == null){
      mEsConfig = Global.createElasticsearchConfig(true);
    }
    mClientSettings = ImmutableSettings.settingsBuilder().put(mEsConfig.getClientSettings())
        .build();
    mClient = new TransportClient(mClientSettings)
        .addTransportAddress(new InetSocketTransportAddress(mEsConfig.getServer(), Integer
            .valueOf(mEsConfig.getJavaPort())));
    mElClient = new ElasticsearchProvider(mClient, mEsConfig);
    mRepo = new BaseRepository(mElClient, Paths.get(System.getProperty("java.io.tmpdir")));
  }

  @Test
  public void testResourceWithIdentifiedSubObject() throws IOException {
    Resource resource = new Resource("Person", "id001");
    String property = "attended";
    Resource value = new Resource("Event", "OER15");
    resource.put(property, value);
    Resource expected1 = getResourceFromJsonFile("BaseRepositoryTest/testResourceWithIdentifiedSubObject.OUT.1.json");
    Resource expected2 = getResourceFromJsonFile("BaseRepositoryTest/testResourceWithIdentifiedSubObject.OUT.2.json");
    mRepo.addResource(resource);
    Assert.assertEquals(expected1, mRepo.getResource("id001"));
    Assert.assertEquals(expected2, mRepo.getResource("OER15"));
  }

  @Test
  public void testResourceWithUnidentifiedSubObject() throws IOException {
    Resource resource = new Resource("Person", "id002");
    Resource value = new Resource("Foo", null);
    resource.put("attended", value);
    Resource expected = getResourceFromJsonFile("BaseRepositoryTest/testResourceWithUnidentifiedSubObject.OUT.1.json");
    mRepo.addResource(resource);
    Assert.assertEquals(expected, mRepo.getResource("id002"));
  }
  
  @AfterClass
  public static void clean() throws IOException {
    ElasticsearchHelpers.cleanIndex(mElClient, mEsConfig.getIndex());
  }
}
