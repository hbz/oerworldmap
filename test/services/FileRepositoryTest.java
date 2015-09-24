package services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import helpers.ElasticsearchTestGrid;
import helpers.UniversalFunctions;
import models.Resource;
import services.repository.FileRepository;

public class FileRepositoryTest extends ElasticsearchTestGrid {

  private static FileRepository resourceRepository;
  private static Resource resource;

  @BeforeClass
  public static void setUpDir() throws IOException {
    UniversalFunctions
        .deleteDirectory(new File(mConfig.getString("filerepo.dir").concat("/Person")));
    resourceRepository = new FileRepository(mConfig);
    resource = new Resource("Person", "1");
    resource.put("name", "John Doe");
    resourceRepository.addResource(resource, "Person");
  }

  @Test
  public void testGetResource() throws IOException {
    Resource fromStore = resourceRepository.getResource("1");
    assertTrue(resource.equals(fromStore));
  }

  @Test
  public void testGetAll() throws IOException {
    List<Resource> results = resourceRepository.getAll("Person");
    assertEquals(results.size(), 1);
  }

  @AfterClass
  public static void tearDownDir() throws IOException {
    UniversalFunctions
        .deleteDirectory(new File(mConfig.getString("filerepo.dir").concat("/Person")));
  }

}
