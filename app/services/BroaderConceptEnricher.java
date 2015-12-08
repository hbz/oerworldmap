package services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.eclipse.jetty.util.ConcurrentHashSet;

import helpers.JsonLdConstants;
import models.Resource;
import play.Logger;
import services.repository.Readable;

/**
 * @author fo, pvb
 */
public class BroaderConceptEnricher implements ResourceEnricher {

  private static final String ABOUT = "about";
  private static final String BROADER = "broader";

  @Override
  public Resource enrich(Resource aToBeEnriched, @Nonnull Readable aEnrichmentSource) {
    final Set<Resource> subjects = new ConcurrentHashSet<>();
    final Set<Resource> broaderResources = new HashSet<>();
    subjects.addAll(aToBeEnriched.getAsList(ABOUT));
    broaderResources.addAll(subjects);
    if (subjects.isEmpty()) {
      return aToBeEnriched;
    }
    try {
      subjects.addAll(traverseBroaderConcept(subjects, broaderResources, aEnrichmentSource));
    } catch (IOException e) {
      Logger.error(e.toString());
    }
    final List<Resource> broaderResourcesAsList = new ArrayList<>();
    broaderResourcesAsList.addAll(broaderResources);
    aToBeEnriched.put(ABOUT, broaderResourcesAsList);
    return aToBeEnriched;
  }

  private static Set<Resource> traverseBroaderConcept(Set<Resource> aResources,
		  Set<Resource> aReturnList, Readable aRepo) throws IOException {
    Iterator<Resource> it = aResources.iterator();
    while (it.hasNext()) {
      Resource next = it.next();
      String id = next.getAsString(JsonLdConstants.ID);
      if (null != id) {
        Resource fromRepo = aRepo.getResource(id);
        if (null != fromRepo) {
          Resource broaderConceptRef = fromRepo.getAsResource(BROADER);
          if (null != broaderConceptRef) {
            String broaderId = broaderConceptRef.getAsString(JsonLdConstants.ID);
            if (null != broaderId) {
              Resource broaderConcept = aRepo.getResource(broaderId);
              if (null != broaderConcept) {
                aReturnList.add(Resource.getIdClone(broaderConcept));
                aResources.add(Resource.getIdClone(broaderConcept));
                aResources.remove(next);
                traverseBroaderConcept(aResources, aReturnList, aRepo);
              }
            }
          }
        }
      }
    }
    return aReturnList;
  }
}
