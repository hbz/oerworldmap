package services;

import helpers.JsonLdConstants;
import models.Resource;
import play.Logger;
import services.repository.Readable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author fo
 */
public class ResourceEnricher {

  public static void enrich(Resource aResource, Readable aRepo) {
    List<Resource> subjects = aResource.getAsList("about");
    if (subjects.isEmpty()) {
      return;
    }
    List<Resource> broaderSubjects = new ArrayList<>();
    broaderSubjects.addAll(subjects);
    for (Resource subject : subjects) {
      try {
        Resource loadedSubject = aRepo.getResource(subject.getAsString(JsonLdConstants.ID));
        if (!(null == loadedSubject)) {
          broaderSubjects.addAll(traverseBroaderConcepts(loadedSubject, aRepo));
        }
      } catch (IOException e) {
        Logger.error(e.toString());
      }
    }
    aResource.put("about", export(broaderSubjects));
  }

  private static List<Resource> traverseBroaderConcepts(Resource aResource, Readable aRepo) {
    List<Resource> broaderConcepts = new ArrayList<>();
    Resource broaderConceptRef = aResource.getAsResource("broader");
    if (!(null == broaderConceptRef)) {
      try {
        Resource broaderConcept = aRepo.getResource(broaderConceptRef.getAsString(JsonLdConstants.ID));
        if (!(null == broaderConcept)) {
          broaderConcepts.add(Resource.getIdClone(broaderConcept));
          broaderConcepts.addAll(traverseBroaderConcepts(broaderConcept, aRepo));
        }
      } catch (IOException e) {
        Logger.error(e.toString());
      }
    }
    return broaderConcepts;
  }

  private static List<Resource> export(List<Resource> aConcepts) {
    List<Resource> uniqueConcepts = new ArrayList<>();
    for (Resource concept: aConcepts) {
      if (!uniqueConcepts.contains(concept)) {
        uniqueConcepts.add(concept);
      }
    }
    return uniqueConcepts;
  }

}
