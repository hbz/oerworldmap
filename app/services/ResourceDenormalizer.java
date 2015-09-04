package services;

import helpers.JsonLdConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import models.DenormalizeResourceWrapper;
import models.Resource;

public class ResourceDenormalizer {

  final private static Map<String, String> mKnownInverseRelations = new HashMap<>();
  static {
    mKnownInverseRelations.put("author", "authorOf");
    mKnownInverseRelations.put("authorOf", "author");
    mKnownInverseRelations.put("member", "memberOf");
    mKnownInverseRelations.put("memberOf", "member");
    mKnownInverseRelations.put("provider", "provides");
    mKnownInverseRelations.put("provides", "provider");
    mKnownInverseRelations.put("creator", "created");
    mKnownInverseRelations.put("created", "creator");
    mKnownInverseRelations.put("agent", "agentIn");
    mKnownInverseRelations.put("agentIn", "agent");
    //mKnownInverseRelations.put("mentions", "mentionedIn");
    //mKnownInverseRelations.put("mentionedIn", "mentions");
    mKnownInverseRelations.put("participant", "participantIn");
    mKnownInverseRelations.put("participantIn", "participant");
  }
  final private static List<String> mListValueEntries = new ArrayList<>();
  static {
    mListValueEntries.add("author");
    mListValueEntries.add("authorOf");
    mListValueEntries.add("member");
    mListValueEntries.add("memberOf");
    mListValueEntries.add("provider");
    mListValueEntries.add("provides");
    mListValueEntries.add("creator");
    mListValueEntries.add("created");
    mListValueEntries.add("agent");
    mListValueEntries.add("agentIn");
    mListValueEntries.add("mentions");
    mListValueEntries.add("mentionedIn");
    mListValueEntries.add("participant");
    mListValueEntries.add("participantIn");
    mListValueEntries.add("narrower");
    mListValueEntries.add("hasTopConcept");
    mListValueEntries.add("about");
    mListValueEntries.add("audience");
  }

  //
  // TODO: add configurable member variables / parameters that specify in what
  // manner exactly Resources should be denormalized ??
  //

  /**
   * 
   */
  private ResourceDenormalizer() { /* no instantiation */
  }

  /**
   * TODO
   * 
   * @param aResource
   * @param aRepo
   * @return
   * @throws IOException
   */
  public static List<Resource> denormalize(Resource aResource, ResourceRepository aRepo)
      throws IOException {

    Map<String, DenormalizeResourceWrapper> wrappedResources = new HashMap<>();
    split(aResource, wrappedResources, aRepo);
    
    addInverseReferences(wrappedResources);
    
    createLinkViews(wrappedResources);
    createEmbedViews(wrappedResources);
    
    return export(wrappedResources);
  }

  private static void createEmbedViews(Map<String, DenormalizeResourceWrapper> aWrappedResources) {
    for (Entry<String, DenormalizeResourceWrapper> wrapperEntry : aWrappedResources.entrySet()){
      wrapperEntry.getValue().createEmbedView(aWrappedResources, mListValueEntries);
    }
  }

  private static void createLinkViews(Map<String, DenormalizeResourceWrapper> aWrappedResources) {
    for (Entry<String, DenormalizeResourceWrapper> wrapperEntry : aWrappedResources.entrySet()){
      wrapperEntry.getValue().createLinkView();
    }
  }

  private static void split(Resource aResource,
      Map<String, DenormalizeResourceWrapper> aWrappedResources, ResourceRepository aRepo)
      throws IOException {
    String keyId = aResource.getAsString(JsonLdConstants.ID);
    if (keyId == null || !aWrappedResources.containsKey(keyId)){
      // we need a new wrapper
      DenormalizeResourceWrapper wrapper = new DenormalizeResourceWrapper(aResource, aWrappedResources, aRepo);
      aWrappedResources.put(wrapper.getKeyId(), wrapper);
    }
    else{
      // take the existing wrapper
      aWrappedResources.get(keyId).addResource(aResource, aWrappedResources);
    }
    
    for (Entry<String, Object> entry : aResource.entrySet()) {
      if (entry.getValue() instanceof Resource) {
        Resource resource = (Resource) entry.getValue();
        if (resource.hasId()){
          split(resource, aWrappedResources, aRepo);
        }
      } else if (entry.getValue() instanceof List) {
        Iterator<?> iter = ((List<?>) entry.getValue()).iterator();
        while (iter.hasNext()) {
          Object next = iter.next();
          if (next instanceof Resource) {
            Resource resource = (Resource) next;
            if (resource.hasId()){
              split(resource, aWrappedResources, aRepo);
            }
          }
        }
      }
    }

  }

  private static void addInverseReferences(Map<String, DenormalizeResourceWrapper> aWrappedResources) {
    for (Entry<String, DenormalizeResourceWrapper> wrapperEntry : aWrappedResources.entrySet()){
      wrapperEntry.getValue().createInverseReferences(aWrappedResources, mKnownInverseRelations);
    }
  }

  private static List<Resource> export(Map<String, DenormalizeResourceWrapper> aWrappedResources) {
    List<Resource> result = new ArrayList<>();
    for (Entry<String, DenormalizeResourceWrapper> wrapperEntry : aWrappedResources.entrySet()){
      result.add(wrapperEntry.getValue().export(aWrappedResources, mListValueEntries));
    }
    return result;
  }

  public static Map<String, String> getKnownInverseRelations() {
    return mKnownInverseRelations;
  }

}
