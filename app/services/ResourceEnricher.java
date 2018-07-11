package services;

import org.apache.jena.rdf.model.Model;

/**
 * @author fo, pvb
 */
public interface ResourceEnricher {

  /**
   * The one method that is crucial for the enrichment of a {@link}Resource.
   *
   * @param aToBeEnriched The Model to be enriched.
   */
  void enrich(Model aToBeEnriched);
}
