package services;

import javax.annotation.Nullable;

import models.Resource;
import services.repository.Readable;

/**
 * @author fo, pvb
 */
public interface ResourceEnricher {

	/**
	 * The one method that is crucial for the enrichment of a {@link}Resource.
	 *
	 * @param aToBeEnriched The Resource to be enriched.
	 * @param aEnrichmentSource The nullable repository that can hold the
	 *            information to be attached to the Resource. However, there can
	 *            be ResourceEnrichers that don't need a such repository, if
	 *            they derive the information exclusively from the resource
	 *            itself or from other (external) sources.
	 * @return the enriched Resource
	 */
	public Resource enrich(Resource aToBeEnriched, @Nullable Readable aEnrichmentSource);

}
