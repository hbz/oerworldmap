import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import models.Resource;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import services.FileResourceRepository;
import services.ResourceRepository;

public class FileResourceRepositoryTest {

  private static Path tmpPath = Paths.get(System.getProperty("java.io.tmpdir"), "resources");

  private ResourceRepository resourceRepository;

  @Before
  public void setUp() throws IOException {
    resourceRepository = new FileResourceRepository(tmpPath);
  }

  @BeforeClass
  public static void setUpDir() throws IOException {
    Files.createDirectory(tmpPath);
  }

  @AfterClass
  public static void tearDownDir() throws IOException {
    deleteDirectory(tmpPath.toFile());
  }

  @Test
  public void testAddGetResource() throws IOException {
    ResourceRepository resourceRepository = new FileResourceRepository(tmpPath);
    Resource resource = new Resource("person", "1");
    resource.set("name", "John Doe");
    resourceRepository.addResource(resource);
    Resource fromStore = resourceRepository.getResource("1");
    assertTrue(resource.equals(fromStore));
  }

  @Test
  public void testQuery() throws IOException {
    ResourceRepository resourceRepository = new FileResourceRepository(tmpPath);
    Resource resource = new Resource("person", "1");
    resource.set("name", "John Doe");
    resourceRepository.addResource(resource);
    List<Resource> results = resourceRepository.query("person");
    assertEquals(results.size(), 1);
  }

  private static boolean deleteDirectory(File path) {
    if (path.exists()) {
      File[] files = path.listFiles();
      for (int i=0; i<files.length; i++) {
         if (files[i].isDirectory()) {
           deleteDirectory(files[i]);
         } else {
           files[i].delete();
         }
      }
    }
    return(path.delete());
  }

}

