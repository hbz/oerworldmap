package services;

import helpers.ElasticsearchTestGrid;
import helpers.UniversalFunctions;
import models.Resource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import services.repository.FileRepository;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FileRepositoryTest extends ElasticsearchTestGrid {

  private static FileRepository resourceRepository;
  private static Resource resource;

  @BeforeClass
  public static void setUpDir() throws IOException {
    UniversalFunctions
        .deleteDirectory(new File(mConfig.getString("filerepo.dir"), "WebPage"));
    resourceRepository = new FileRepository(mConfig);
    resource = new Resource("Person", "1");
    resource.put("name", "John Doe");
    resourceRepository.addItem(resource, new HashMap<>());
  }

  @Test
  public void testGetResource() throws IOException {
    Resource fromStore = resourceRepository.getItem("1");
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
        .deleteDirectory(new File(mConfig.getString("filerepo.dir"), "WebPage"));
  }

}
