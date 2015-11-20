package services;

import static org.junit.Assert.assertEquals;

import helpers.JsonTest;
import models.Resource;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

/**
 * @author fo
 */
public class ResourceEnricherTest implements JsonTest {
	
  final private static BroaderConceptEnricher mBroaderConceptEnricher = new BroaderConceptEnricher();

  @Test
  public void testEnrichBroaderConcepts() throws IOException {

    Resource in = getResourceFromJsonFile(
      "ResourceEnricherTest/testEnrichBroaderConcepts.IN.json");
    Resource db1 = getResourceFromJsonFile(
      "ResourceEnricherTest/testEnrichBroaderConcepts.DB.1.json");
    Resource db2 = getResourceFromJsonFile(
      "ResourceEnricherTest/testEnrichBroaderConcepts.DB.2.json");
    Resource db3 = getResourceFromJsonFile(
      "ResourceEnricherTest/testEnrichBroaderConcepts.DB.3.json");
    Resource out = getResourceFromJsonFile(
      "ResourceEnricherTest/testEnrichBroaderConcepts.OUT.json");

    MockResourceRepository repo = new MockResourceRepository();
    repo.addResource(db1);
    repo.addResource(db2);
    repo.addResource(db3);

    mBroaderConceptEnricher.enrich(in, repo);
    assertEquals(out, in);

  }

  @Test
  public void testEnrichBroaderESCConcepts() throws IOException {

    Resource in = getResourceFromJsonFile(
      "ResourceEnricherTest/testEnrichBroaderESCConcepts.IN.json");
    Resource esc = getResourceFromJsonFile(
      "ResourceEnricherTest/testEnrichBroaderESCConcepts.DB.json");
    Resource out = getResourceFromJsonFile(
      "ResourceEnricherTest/testEnrichBroaderESCConcepts.OUT.json");

    MockResourceRepository repo = new MockResourceRepository();
    List<Resource> denormalized = ResourceDenormalizer.denormalize(esc, repo);
    for (Resource resource : denormalized) {
      repo.addResource(resource);
    }

    mBroaderConceptEnricher.enrich(in, repo);
    assertEquals(out, in);

  }

}
