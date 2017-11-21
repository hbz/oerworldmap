package services;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import helpers.JsonLdConstants;
import helpers.JsonTest;
import models.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import services.repository.TriplestoreRepository;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * Created by fo on 10.12.15.
 */
public class TriplestoreRepositoryTest implements JsonTest {

  private Config mConfig = ConfigFactory.load(ClassLoader.getSystemClassLoader(), "test.conf");
  private static Map<String, Object> mMetadata = new HashMap<>();

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
    triplestoreRepository.addItem(resource1, mMetadata);
    triplestoreRepository.addItem(resource2, mMetadata);

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
    triplestoreRepository.addItem(resource1, mMetadata);
    triplestoreRepository.addItem(resource2, mMetadata);
    triplestoreRepository.addItem(resource3, mMetadata);

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
    triplestoreRepository.addItem(resource1, mMetadata);
    triplestoreRepository.addItem(resource2, mMetadata);
    triplestoreRepository.addItem(update1, mMetadata);

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
    triplestoreRepository.addItem(resource1, mMetadata);
    triplestoreRepository.addItem(resource2, mMetadata);
    triplestoreRepository.addItem(resource3, mMetadata);
    triplestoreRepository.addItem(update1, mMetadata);

    Model expected = ModelFactory.createDefaultModel();
    RDFDataMgr.read(expected, "TriplestoreRepositoryTest/testUpdateResourceWithReferences.IN.1.nt", Lang.NTRIPLES);

    assertTrue(actual.isIsomorphicWith(expected));

  }

  @Test
  public void testGetResource() throws IOException {

    Resource resource1 = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testAddResource.IN.1.json");

    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(mConfig);
    triplestoreRepository.addItem(resource1, mMetadata);

    Resource back = (Resource) triplestoreRepository.getItem(resource1.getId());
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
    triplestoreRepository.addItem(resource1, mMetadata);
    triplestoreRepository.addItem(resource2, mMetadata);
    triplestoreRepository.addItem(resource3, mMetadata);
    triplestoreRepository.addItem(update1, mMetadata);

    Resource expected = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testGetUpdatedResource.OUT.1.json");
    Resource back = (Resource) triplestoreRepository.getItem(resource1.getId());

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
    triplestoreRepository.addItem(resource1, mMetadata);
    triplestoreRepository.addItem(resource2, mMetadata);
    triplestoreRepository.addItem(resource3, mMetadata);

    assertNotNull(triplestoreRepository.getItem("info:alice"));
    assertNotNull(triplestoreRepository.getItem("info:bob"));
    assertNotNull(triplestoreRepository.getItem("info:carol"));
    assertEquals(10, actual.size());

    triplestoreRepository.deleteItem("info:alice", Record.class, mMetadata);

    assertNull(triplestoreRepository.getItem("info:alice"));
    assertNotNull(triplestoreRepository.getItem("info:bob"));
    assertNotNull(triplestoreRepository.getItem("info:carol"));
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

    triplestoreRepository.addItem(in1, mMetadata);
    triplestoreRepository.addItem(in2, mMetadata);
    triplestoreRepository.addItem(in3, mMetadata);


    // delete affiliation "Oh No Company" and check whether it has been removed
    // from referencing resources
    Resource toBeDeleted = (Resource) triplestoreRepository.getItem("info:urn:uuid:49d8b330-e3d5-40ca-b5cb-2a8dfca70987");
    triplestoreRepository.deleteItem(toBeDeleted.getAsString(JsonLdConstants.ID), Record.class, mMetadata);

    Resource result1 = (Resource) triplestoreRepository.getItem("info:urn:uuid:49d8b330-e3d5-40ca-b5cb-2a8dfca70456");
    Resource result2 = (Resource) triplestoreRepository.getItem("info:urn:uuid:49d8b330-e3d5-40ca-b5cb-2a8dfca70123");
    Assert.assertEquals(expected1, result1);
    Assert.assertEquals(expected2, result2);
    Assert.assertNull(triplestoreRepository.getItem("info:urn:uuid:49d8b330-e3d5-40ca-b5cb-2a8dfca70987"));
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
    triplestoreRepository.addItem(resource1, mMetadata);
    triplestoreRepository.addItem(resource2, mMetadata);
    triplestoreRepository.addItem(resource3, mMetadata);

    List<ModelCommon> resources = triplestoreRepository.getAll("http://schema.org/Person");

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

    Resource staged1 = (Resource) triplestoreRepository.stage(resource1);

    assertEquals(resource1, staged1);
    assertTrue(actual.isEmpty());

    triplestoreRepository.addItem(staged1, mMetadata);

    Resource staged2 = (Resource) triplestoreRepository.stage(update1);

    assertEquals(triplestoreRepository.getItem("info:alice"), resource1);
    assertEquals(update1, staged2);

  }

  @Test
  public void testStageWithBnodeInSelfReference() throws IOException {

    Resource resource = getResourceFromJsonFile(
      "TriplestoreRepositoryTest/testStageWithSelfReference.IN.1.json");

    Model actual = ModelFactory.createDefaultModel();
    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(mConfig, actual);
    triplestoreRepository.addItem(resource, mMetadata);

    Resource staged = (Resource) triplestoreRepository.stage(resource);

    assertEquals(resource, staged);

  }

  @Test
  public void testUpdateDelete() throws IOException {

    Model db = ModelFactory.createDefaultModel();
    RDFDataMgr.read(db, "TriplestoreRepositoryTest/testUpdate.IN.nt", Lang.NTRIPLES);
    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(mConfig, db);

    Commit.Diff actual = triplestoreRepository.update(
      "?s <info:objectProperty> ?o", null, "?s <info:objectProperty> ?o"
    );
    Commit.Diff expected = TripleCommit.Diff.fromString(
      "- <info:subject> <info:objectProperty> <info:object> .\n"
    );

    assertEquals(expected.toString(), actual.toString());

  }

  @Test
  public void testUpdateInsert() throws IOException {

    Model db = ModelFactory.createDefaultModel();
    RDFDataMgr.read(db, "TriplestoreRepositoryTest/testUpdate.IN.nt", Lang.NTRIPLES);
    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(mConfig, db);

    Commit.Diff actual = triplestoreRepository.update(
      null, "?s <info:anotherProperty> ?o", "?s <info:objectProperty> ?o"
    );
    Commit.Diff expected = TripleCommit.Diff.fromString(
      "+ <info:subject> <info:anotherProperty> <info:object> .\n"
    );

    assertEquals(expected.toString(), actual.toString());

  }

  @Test
  public void testUpdateDeleteInsert() throws IOException {

    Model db = ModelFactory.createDefaultModel();
    RDFDataMgr.read(db, "TriplestoreRepositoryTest/testUpdate.IN.nt", Lang.NTRIPLES);
    TriplestoreRepository triplestoreRepository = new TriplestoreRepository(mConfig, db);

    Commit.Diff actual = triplestoreRepository.update(
      "?s <info:objectProperty> ?o", "?s <info:anotherProperty> ?o", "?s <info:objectProperty> ?o"
    );
    Commit.Diff expected = TripleCommit.Diff.fromString(
      "- <info:subject> <info:objectProperty> <info:object> .\n" +
      "+ <info:subject> <info:anotherProperty> <info:object> .\n"
    );

    assertEquals(expected.toString(), actual.toString());

  }

}
