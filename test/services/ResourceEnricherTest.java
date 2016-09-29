package services;

import static org.junit.Assert.assertTrue;

import org.apache.jena.rdf.model.Model;
import helpers.JsonTest;
import org.junit.Test;

import java.io.IOException;

/**
 * @author fo
 */
public class ResourceEnricherTest implements JsonTest {

  final private static BroaderConceptEnricher mBroaderConceptEnricher = new BroaderConceptEnricher();

  @Test
  public void testEnrichBroaderESCConcepts() throws IOException {

    Model in = getResourceFromJsonFile(
      "ResourceEnricherTest/testEnrichBroaderESCConcepts.IN.json").toModel();
    Model out = getResourceFromJsonFile(
      "ResourceEnricherTest/testEnrichBroaderESCConcepts.OUT.json").toModel();

    mBroaderConceptEnricher.enrich(in);
    assertTrue(in.isIsomorphicWith(out));

  }

}
