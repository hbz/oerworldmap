package services;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import models.Resource;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import services.ElasticsearchClient;
import services.ElasticsearchRepository;
import services.ElasticsearchConfig;

public class ElasticsearchRepositoryTest {

  private static Resource mResource1;
  private static Resource mResource2;
  private static ElasticsearchClient mElClient;
  private static ElasticsearchRepository mRepo;
  private static final ElasticsearchConfig esConfig = new ElasticsearchConfig();

  @BeforeClass
  public static void setup() throws IOException {
    mResource1 = new Resource(esConfig.getType(), UUID.randomUUID().toString());
    mResource1.put("name", "oeruser1");
    mResource1.put("worksFor", "oerknowledgecloud.org");

    mResource2 = new Resource(esConfig.getType(), UUID.randomUUID().toString());
    mResource2.put("name", "oeruser2");
    mResource2.put("worksFor", "unesco.org");

    mElClient = new ElasticsearchClient(nodeBuilder().settings(esConfig.getClientSettings()).local(true).node()
        .client());
    cleanIndex();
    mRepo = new ElasticsearchRepository(mElClient);
  }

  // create a new clean ElasticsearchIndex for this Test class
  private static void cleanIndex() {
    if (mElClient.hasIndex(esConfig.getIndex())){
      mElClient.deleteIndex(esConfig.getIndex());
    }
    mElClient.createIndex(esConfig.getIndex());
  }

  @Test
  public void testAddAndQueryResources() throws IOException {
    mRepo.addResource(mResource1);
    mRepo.addResource(mResource2);
    mElClient.refreshIndex(esConfig.getIndex());

    List<Resource> resourcesGotBack = mRepo.query(esConfig.getType());

    Assert.assertTrue(resourcesGotBack.contains(mResource1));
    Assert.assertTrue(resourcesGotBack.contains(mResource2));
  }
}
