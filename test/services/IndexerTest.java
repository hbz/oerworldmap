package services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.common.collect.Sets;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import helpers.JsonTest;

import models.TripleCommit;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import services.repository.Repository;

import java.io.IOException;
import java.util.Set;

/**
 * Created by fo on 11.02.16.
 */
public class IndexerTest implements JsonTest {

  @Test
  public void testIndexNewResourceWithNewReference() throws IOException {

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
    ResourceIndexer indexer = new ResourceIndexer(db, mockResourceRepository);
    indexer.index(commit);

    // Check presence of indexed resources
    assertNull(mockResourceRepository.getResource("http://schema.org/Article"));
    assertNull(mockResourceRepository.getResource("http://schema.org/Person"));
    assertNotNull(mockResourceRepository.getResource("info:789"));
    assertNotNull(mockResourceRepository.getResource("info:456"));
    assertNotNull(mockResourceRepository.getResource("info:123"));

  }

}
