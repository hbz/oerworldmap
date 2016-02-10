package services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import helpers.JsonTest;
import io.apigee.trireme.core.NodeEnvironment;
import io.apigee.trireme.core.NodeScript;
import io.apigee.trireme.core.ScriptFuture;
import models.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import services.repository.TriplestoreRepository;

import java.io.File;
import java.io.IOException;


/**
 * Created by fo on 10.12.15.
 */
public class TriplestoreRepositoryTest implements JsonTest {

  private Config config = ConfigFactory.load(ClassLoader.getSystemClassLoader(), "test.conf");

  private static ScriptFuture framer;

  @BeforeClass
  public static void setUp() throws Exception {
    NodeEnvironment env = new NodeEnvironment();
    NodeScript script = env.createScript("frame.js",
      new File("node/json-frame/frame.js"), null);
    script.setNodeVersion("0.10");
    framer = script.executeModule();
    framer.getModuleResult();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    framer.cancel(true);
  }

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
      "SchemaTest/testPerson.json");

    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(config);
    triplestoreRepository.addResource(resource1, "Person");

    Resource back = triplestoreRepository.getResource(resource1.getId());
    // FIXME: remove when proper @context is returned
    resource1.remove("@context");
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

}
