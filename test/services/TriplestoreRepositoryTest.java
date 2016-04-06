package services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import helpers.JsonTest;
import models.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Test;
import services.repository.TriplestoreRepository;

import java.io.IOException;
import java.util.List;


/**
 * Created by fo on 10.12.15.
 */
public class TriplestoreRepositoryTest implements JsonTest {

  private Config config = ConfigFactory.load(ClassLoader.getSystemClassLoader(), "test.conf");

  @Test
  public void testAddResource() throws IOException {

    Resource resource1 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.1.json");
    Resource resource2 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.2.json");

    Model actual = ModelFactory.createDefaultModel();
    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(config, actual);
    triplestoreRepository.addResource(resource1, "Person");
    triplestoreRepository.addResource(resource2, "Person");

    Model expected = ModelFactory.createDefaultModel();
    RDFDataMgr.read(expected, "TriplestoreRepositoryTest/testAddResource.IN.1.nt", Lang.NTRIPLES);

    assertTrue(actual.isIsomorphicWith(expected));

  }

  @Test
  public void testAddResourceWithReferences() throws IOException {

    Resource resource1 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.1.json");
    Resource resource2 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.2.json");
    Resource resource3 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResourceWithReferences.IN.1.json");

    Model actual = ModelFactory.createDefaultModel();
    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(config, actual);
    triplestoreRepository.addResource(resource1, "Person");
    triplestoreRepository.addResource(resource2, "Person");
    triplestoreRepository.addResource(resource3, "Person");

    Model expected = ModelFactory.createDefaultModel();
    RDFDataMgr.read(expected, "TriplestoreRepositoryTest/testAddResourceWithReferences.IN.1.nt", Lang.NTRIPLES);

    assertTrue(actual.isIsomorphicWith(expected));

  }

  @Test
  public void testUpdateResource() throws IOException {

    Resource resource1 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.1.json");
    Resource resource2 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.2.json");
    Resource update1 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testUpdateResource.IN.1.json");

    Model actual = ModelFactory.createDefaultModel();
    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(config, actual);
    triplestoreRepository.addResource(resource1, "Person");
    triplestoreRepository.addResource(resource2, "Person");
    triplestoreRepository.addResource(update1, "Person");

    Model expected = ModelFactory.createDefaultModel();
    RDFDataMgr.read(expected, "TriplestoreRepositoryTest/testUpdateResource.IN.1.nt", Lang.NTRIPLES);

    assertTrue(actual.isIsomorphicWith(expected));

  }

  @Test
  public void testUpdateResourceWithReferences() throws IOException {

    Resource resource1 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.1.json");
    Resource resource2 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.2.json");
    Resource resource3 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResourceWithReferences.IN.1.json");
    Resource update1 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testUpdateResourceWithReferences.IN.1.json");

    Model actual = ModelFactory.createDefaultModel();
    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(config, actual);
    triplestoreRepository.addResource(resource1, "Person");
    triplestoreRepository.addResource(resource2, "Person");
    triplestoreRepository.addResource(resource3, "Person");
    triplestoreRepository.addResource(update1, "Person");

    Model expected = ModelFactory.createDefaultModel();
    RDFDataMgr.read(expected, "TriplestoreRepositoryTest/testUpdateResourceWithReferences.IN.1.nt", Lang.NTRIPLES);

    assertTrue(actual.isIsomorphicWith(expected));

  }

  @Test
  public void testGetResource() throws IOException {

    Resource resource1 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.1.json");

    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(config);
    triplestoreRepository.addResource(resource1, "Person");

    Resource back = triplestoreRepository.getResource(resource1.getId());
    assertEquals(resource1, back);

  }

  @Test
  public void testGetUpdatedResource() throws IOException {

    Resource resource1 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.1.json");
    Resource resource2 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.2.json");
    Resource resource3 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResourceWithReferences.IN.1.json");
    Resource update1 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testUpdateResourceWithReferences.IN.1.json");

    Model actual = ModelFactory.createDefaultModel();
    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(config, actual);
    triplestoreRepository.addResource(resource1, "Person");
    triplestoreRepository.addResource(resource2, "Person");
    triplestoreRepository.addResource(resource3, "Person");
    triplestoreRepository.addResource(update1, "Person");

    Resource expected = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testGetUpdatedResource.OUT.1.json");
    Resource back = triplestoreRepository.getResource(resource1.getId());

    assertEquals(expected, back);

  }

  @Test
  public void testDeleteResource() throws IOException {

    Resource resource1 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.1.json");
    Resource resource2 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.2.json");
    Resource resource3 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResourceWithReferences.IN.1.json");

    Model actual = ModelFactory.createDefaultModel();
    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(config, actual);
    triplestoreRepository.addResource(resource1, "Person");
    triplestoreRepository.addResource(resource2, "Person");
    triplestoreRepository.addResource(resource3, "Person");

    assertNotNull(triplestoreRepository.getResource("info:alice"));
    assertNotNull(triplestoreRepository.getResource("info:bob"));
    assertNotNull(triplestoreRepository.getResource("info:carol"));
    assertEquals(10, actual.size());

    triplestoreRepository.deleteResource("info:alice");

    assertNull(triplestoreRepository.getResource("info:alice"));
    assertNotNull(triplestoreRepository.getResource("info:bob"));
    assertNotNull(triplestoreRepository.getResource("info:carol"));
    assertEquals(6, actual.size());

  }

  @Test
  public void testGetAll() throws IOException {

    Resource resource1 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.1.json");
    Resource resource2 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.2.json");
    Resource resource3 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResourceWithReferences.IN.1.json");

    Model actual = ModelFactory.createDefaultModel();
    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(config, actual);
    triplestoreRepository.addResource(resource1, "Person");
    triplestoreRepository.addResource(resource2, "Person");
    triplestoreRepository.addResource(resource3, "Person");

    List<Resource> resources = triplestoreRepository.getAll("http://schema.org/Person");

    assertEquals(3, resources.size());

  }

}
