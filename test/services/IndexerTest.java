package services;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Sets;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import helpers.JsonTest;

import models.TripleCommit;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

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

    // Read diff
    String diffString = IOUtils.toString(
      ClassLoader.getSystemResourceAsStream("IndexerTest/testNewResourceWithNewReference.IN.ndiff"), "UTF-8");
    TripleCommit.Diff diff = TripleCommit.Diff.fromString(diffString);

    // Apply diff to populate DB
    diff.apply(db);

    // Calculate scope of resources to be (re-)indexed
    ResourceIndexer indexer = new ResourceIndexer(db);
    Set<String> idsToIndex = indexer.getScope(diff);

    assertEquals(3, idsToIndex.size());
    Set expected = Sets.newHashSet("info:789", "info:456", "info:123");
    assertEquals(expected, idsToIndex);

  }

}
