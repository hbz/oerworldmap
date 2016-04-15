package services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import helpers.JsonLdConstants;
import helpers.JsonTest;
import models.Resource;
import models.TripleCommit;
import services.repository.TriplestoreRepository;


/**
 * Created by fo on 10.12.15.
 */
public class TriplestoreRepositoryTest implements JsonTest {

  private Config mConfig = ConfigFactory.load(ClassLoader.getSystemClassLoader(), "test.conf");
  private static Map<String, String> mMetadata = new HashMap<>();

  @BeforeClass
  public static void setUp() {
    mMetadata.put(TripleCommit.Header.AUTHOR_HEADER, "Anonymous");
    mMetadata.put(TripleCommit.Header.DATE_HEADER, "2016-04-08T17:34:37.038+02:00");
  }

  @Test
  public void testAddResource() throws IOException {

    Resource resource1 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.1.json");
    Resource resource2 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.2.json");

    Model actual = ModelFactory.createDefaultModel();
    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(mConfig, actual);
    triplestoreRepository.addResource(resource1, mMetadata);
    triplestoreRepository.addResource(resource2, mMetadata);

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
    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(mConfig, actual);
    triplestoreRepository.addResource(resource1, mMetadata);
    triplestoreRepository.addResource(resource2, mMetadata);
    triplestoreRepository.addResource(resource3, mMetadata);

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
    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(mConfig, actual);
    triplestoreRepository.addResource(resource1, mMetadata);
    triplestoreRepository.addResource(resource2, mMetadata);
    triplestoreRepository.addResource(update1, mMetadata);

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
    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(mConfig, actual);
    triplestoreRepository.addResource(resource1, mMetadata);
    triplestoreRepository.addResource(resource2, mMetadata);
    triplestoreRepository.addResource(resource3, mMetadata);
    triplestoreRepository.addResource(update1, mMetadata);

    Model expected = ModelFactory.createDefaultModel();
    RDFDataMgr.read(expected, "TriplestoreRepositoryTest/testUpdateResourceWithReferences.IN.1.nt", Lang.NTRIPLES);

    assertTrue(actual.isIsomorphicWith(expected));

  }

  @Test
  public void testGetResource() throws IOException {

    Resource resource1 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.1.json");

    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(mConfig);
    triplestoreRepository.addResource(resource1, mMetadata);

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
    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(mConfig, actual);
    triplestoreRepository.addResource(resource1, mMetadata);
    triplestoreRepository.addResource(resource2, mMetadata);
    triplestoreRepository.addResource(resource3, mMetadata);
    triplestoreRepository.addResource(update1, mMetadata);

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
    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(mConfig, actual);
    triplestoreRepository.addResource(resource1, mMetadata);
    triplestoreRepository.addResource(resource2, mMetadata);
    triplestoreRepository.addResource(resource3, mMetadata);

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
  public void testDeleteResourceWithMentionedResources() throws IOException {
    // setup: 1 Person ("in1") who has 2 affiliations
    Resource in1 = getResourceFromJsonFile("BaseRepositoryTest/testDeleteResourceWithMentionedResources.IN.1.json");
    Resource in2 = getResourceFromJsonFile("BaseRepositoryTest/testDeleteResourceWithMentionedResources.IN.2.json");
    Resource in3 = getResourceFromJsonFile("BaseRepositoryTest/testDeleteResourceWithMentionedResources.IN.3.json");
    Resource expected1 = getResourceFromJsonFile(
      "BaseRepositoryTest/testDeleteResourceWithMentionedResources.OUT.1.json");
    Resource expected2 = getResourceFromJsonFile(
      "BaseRepositoryTest/testDeleteResourceWithMentionedResources.OUT.2.json");

    Model actual = ModelFactory.createDefaultModel();
    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(mConfig, actual);

    triplestoreRepository.addResource(in1, mMetadata);
    triplestoreRepository.addResource(in2, mMetadata);
    triplestoreRepository.addResource(in3, mMetadata);


    // delete affiliation "Oh No Company" and check whether it has been removed
    // from referencing resources
    Resource toBeDeleted = triplestoreRepository.getResource("info:urn:uuid:49d8b330-e3d5-40ca-b5cb-2a8dfca70987");
    triplestoreRepository.deleteResource(toBeDeleted.getAsString(JsonLdConstants.ID));

    Resource result1 = triplestoreRepository.getResource("info:urn:uuid:49d8b330-e3d5-40ca-b5cb-2a8dfca70456");
    Resource result2 = triplestoreRepository.getResource("info:urn:uuid:49d8b330-e3d5-40ca-b5cb-2a8dfca70123");
    Assert.assertEquals(expected1, result1);
    Assert.assertEquals(expected2, result2);
    Assert.assertNull(triplestoreRepository.getResource("info:urn:uuid:49d8b330-e3d5-40ca-b5cb-2a8dfca70987"));
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
    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(mConfig, actual);
    triplestoreRepository.addResource(resource1, mMetadata);
    triplestoreRepository.addResource(resource2, mMetadata);
    triplestoreRepository.addResource(resource3, mMetadata);

    List<Resource> resources = triplestoreRepository.getAll("http://schema.org/Person");

    assertEquals(3, resources.size());

  }

  @Test
  public void testStage() throws IOException {

    Resource resource1 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.1.json");

    Resource update1 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testUpdateResource.IN.1.json");

    Model actual = ModelFactory.createDefaultModel();
    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(mConfig, actual);

    Resource staged1 = triplestoreRepository.stage(resource1);

    assertEquals(resource1, staged1);
    assertTrue(actual.isEmpty());

    triplestoreRepository.addResource(staged1, mMetadata);

    Resource staged2 = triplestoreRepository.stage(update1);

    assertEquals(triplestoreRepository.getResource("info:alice"), resource1);
    assertEquals(update1, staged2);

  }

}
