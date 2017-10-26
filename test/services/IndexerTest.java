package services;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.typesafe.config.ConfigFactory;
import helpers.JsonTest;
import helpers.Types;
import models.TripleCommit;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.BeforeClass;
import org.junit.Test;
import play.Configuration;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Created by fo on 11.02.16.
 */
public class IndexerTest implements JsonTest {

  private static Types mTypes;

  @BeforeClass
  public static void setup() throws IOException, ProcessingException {
    mTypes = new Types(new Configuration(
      ConfigFactory.parseFile(new File("conf/test.conf")).resolve()).underlying());
  }

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
    ResourceIndexer indexer = new ResourceIndexer(db, mockResourceRepository, null, mTypes);
    indexer.index(commit.getDiff());

    // Check presence of indexed resources
    assertNull(mockResourceRepository.getItem("http://schema.org/Article"));
    assertNull(mockResourceRepository.getItem("http://schema.org/Person"));
    assertNotNull(mockResourceRepository.getItem("info:urn:uuid:58ea1dfc-23bb-11e5-8892-001999ac0789"));
    assertNotNull(mockResourceRepository.getItem("info:urn:uuid:58ea1dfc-23bb-11e5-8892-001999ac0456"));
    assertNotNull(mockResourceRepository.getItem("info:urn:uuid:58ea1dfc-23bb-11e5-8892-001999ac0123"));

  }

}
