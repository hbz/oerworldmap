package services;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import helpers.JsonTest;
import models.TripleCommit;

/**
 * Created by fo on 11.02.16.
 */
public class IndexerTest implements JsonTest {

  @Test
  public void testIndexNewResourceWithNewReference() throws IOException {

    //ResourceFramer.start();

    // The model for the indexer to SPARQL against
    Model db = ModelFactory.createDefaultModel();

    // Read commit
    String commitString = IOUtils.toString(
        ClassLoader.getSystemResourceAsStream("IndexerTest/testNewResourceWithNewReference.IN.ndiff"), "UTF-8");
    TripleCommit commit = TripleCommit.fromString(commitString);

    // Apply diff to populate DB
    commit.getDiff().apply(db);

    // Index diff to mock repo
    MockResourceRepository mockResourceRepository = new MockResourceRepository();
    ResourceIndexer indexer = new ResourceIndexer(db, mockResourceRepository, null);
    indexer.index(commit.getDiff());

    // Check presence of indexed resources
    assertNull(mockResourceRepository.getResource("http://schema.org/Article"));
    assertNull(mockResourceRepository.getResource("http://schema.org/Person"));
    assertNotNull(mockResourceRepository.getResource("info:urn:uuid:58ea1dfc-23bb-11e5-8892-001999ac0789"));
    assertNotNull(mockResourceRepository.getResource("info:urn:uuid:58ea1dfc-23bb-11e5-8892-001999ac0456"));
    assertNotNull(mockResourceRepository.getResource("info:urn:uuid:58ea1dfc-23bb-11e5-8892-001999ac0123"));

  }

}
