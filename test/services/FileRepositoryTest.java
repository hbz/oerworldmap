package services;

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
import org.junit.BeforeClass;
import org.junit.Test;
import services.repository.FileRepository;

public class FileRepositoryTest {

  private static Path tmpPath = Paths.get(System.getProperty("java.io.tmpdir"), "resources");
  private static FileRepository resourceRepository;
  private static Resource resource;

  @BeforeClass
  public static void setUpDir() throws IOException {
    Files.createDirectory(tmpPath);
    resourceRepository = new FileRepository(tmpPath);
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
    deleteDirectory(tmpPath.toFile());
  }

  private static boolean deleteDirectory(File path) {
    if (path != null && path.exists()) {
      for (File file : path.listFiles()){
        if (file.isDirectory()){
          deleteDirectory(file);
        }
        else{
          file.delete();
        }
      }
      return(path.delete());
    }
    return false;
  }
}

