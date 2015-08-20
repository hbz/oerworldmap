package models;

import helpers.JsonLdConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import controllers.routes.ref;
import services.ResourceDenormalizer;
import services.ResourceRepository;

/**
 * Provides a wrapper around a Resource that has to be separated from, merged
 * with or referenced by one or more other Resource's. The idea is to allow
 * operations with any number of sub or super level Resource's, as long as the
 * other level's Resource has got a different id.
 * 
 * (Whereas direct operations on Resource's can either lead infinitive reference
 * loops or be incomplete with respect to the number of references they should
 * provide.)
 * 
 * @author pvb
 *
 */
public class DenormalizeResourceWrapper {

  private String mKeyId;
  private Resource mResource;
  private Map<String, Set<String>> mReferences;
  private Resource mLinkView;
  private Resource mEmbedView;

  /*
   * 
   */
  public DenormalizeResourceWrapper(final Resource aResource,
      final Map<String, DenormalizeResourceWrapper> aWrappedResources,
      final ResourceRepository aRepo) throws IOException {
    mReferences = new HashMap<>();
    mKeyId = aResource.getAsString(JsonLdConstants.ID);
    if (mKeyId == null) {
      // this is an unidentified new data set, build new wrapper
      mKeyId = UUID.randomUUID().toString();
    }
    mResource = aRepo.getResource(mKeyId);
    if (mResource == null) {
      // this is NOT a modification of an existing data set, build new data set
      mResource = new Resource(aResource.getAsString(JsonLdConstants.TYPE), mKeyId);
    } else {
      // other Resources contained in the repo will be affected by changes on
      // this Resource (wrapper), so add them to the wrappers map.
      getMentionedResources(aWrappedResources, mResource, aRepo, 2);
    }
    addResource(aResource, aWrappedResources);
    mLinkView = null;
    mEmbedView = null;
  }

  private DenormalizeResourceWrapper(final Resource aResource,
      final Map<String, DenormalizeResourceWrapper> aWrappedResources,
      final ResourceRepository aRepo, final int aSubLevels) throws IOException {
    mReferences = new HashMap<>();
    mKeyId = aResource.getAsString(JsonLdConstants.ID);
    mResource = aRepo.getResource(mKeyId);
    if (aSubLevels > -1) {
      getMentionedResources(aWrappedResources, mResource, aRepo, aSubLevels);
    }
    mLinkView = null;
    mEmbedView = null;
  }

  /*
   * 
   */
  private void getMentionedResources(
      final Map<String, DenormalizeResourceWrapper> aWrappedResources, final Resource aResource,
      final ResourceRepository aRepo, final int aSubLevels) throws IOException {
    for (Entry<String, Object> entry : aResource.entrySet()) {
      if (entry.getValue() instanceof Resource) {
        Resource resource = (Resource) entry.getValue();
        if (resource.hasId()) {
          putResourceToWrapperList(aWrappedResources, aRepo, aSubLevels, resource);
          putReference(entry.getKey(), resource.getAsString(JsonLdConstants.ID));
        }
      } //
      else if (entry.getValue() instanceof List) {
        Set<String> ids = new HashSet<>();
        Iterator<?> iter = ((List<?>) entry.getValue()).iterator();
        while (iter.hasNext()) {
          Object next = iter.next();
          if (next instanceof Resource) {
            Resource resource = (Resource) next;
            if (resource.hasId()) {
              ids.add(resource.getAsString(JsonLdConstants.ID));
              putResourceToWrapperList(aWrappedResources, aRepo, aSubLevels, resource);
            }
          }
        }
        putReference(entry.getKey(), ids);
      }
    }
  }

  private Set<String> putReference(String aKey, Set<String> aValue) {
    return mReferences.put(aKey, aValue);
  }

  private Set<String> putReference(String aKey, String aValue) {
    Set<String> references = new HashSet<>();
    references.add(aValue);
    return putReference(aKey, references);
  }

  /*
   * 
   */
  private static void putResourceToWrapperList(
      final Map<String, DenormalizeResourceWrapper> aWrappedResources,
      final ResourceRepository aRepo, final int aSubLevels, Resource resource) throws IOException {
    String id = resource.getAsString(JsonLdConstants.ID);
    if (id != null && !aWrappedResources.containsKey(id)) {
      aWrappedResources.put(id, new DenormalizeResourceWrapper(resource, aWrappedResources, aRepo,
          aSubLevels - 1));
    }
  }

  /**
   * TODO
   * 
   * @param aResource
   * @param aWrappedResources
   */
  public void addResource(Resource aResource,
      Map<String, DenormalizeResourceWrapper> aWrappedResources) {
    extractFirstLevelReferences(aResource, aWrappedResources);
    mResource.merge(Resource.getFlatClone(aResource));
    mLinkView = null;
    mEmbedView = null;
  }

  /*
   * 
   */
  private void extractFirstLevelReferences(Resource aResource,
      Map<String, DenormalizeResourceWrapper> aWrappedResources) {
    for (final Entry<String, Object> entry : aResource.entrySet()) {
      Set<String> oldReferences = new HashSet<>();
      if (entry.getValue() instanceof Resource) {
        Resource resource = (Resource) entry.getValue();
        if (resource.hasId()) {
          oldReferences = putReference(entry.getKey(), resource.getAsString(JsonLdConstants.ID));
        }
      } //
      else if (entry.getValue() instanceof List) {
        Set<String> ids = new HashSet<>();
        Iterator<?> iter = ((List<?>) entry.getValue()).iterator();
        while (iter.hasNext()) {
          Object next = iter.next();
          if (next instanceof Resource) {
            Resource resource = (Resource) next;
            if (resource.hasId()) {
              ids.add(resource.getAsString(JsonLdConstants.ID));
            }
          }
        }
        oldReferences = putReference(entry.getKey(), ids);
      }
      if (oldReferences != null && !oldReferences.isEmpty()) {
        removeOldReferences(entry.getKey(), oldReferences, aWrappedResources);
      }
    }
  }

  private void removeOldReferences(String aKey, Set<String> aReferences,
      Map<String, DenormalizeResourceWrapper> aWrappedResources) {
    if (aReferences == null) {
      return;
    }
    Iterator<String> iter = aReferences.iterator();
    while (iter.hasNext()) {
      String next = iter.next();
      Set<String> newReferences = mReferences.get(aKey);
      if (!newReferences.contains(next)) {
        aWrappedResources.get(next).removeReference(
            ResourceDenormalizer.getKnownInverseRelations().get(aKey), mKeyId);
      }
    }
  }

  private void removeReference(String aKey, String aRefId) {
    Set<String> references = mReferences.get(aKey);
    if (references != null) {
      references.remove(aRefId);
      mReferences.put(aKey, references);
    }
  }

  /**
   * Adds additional reference data to another wrapper, providing the key by
   * which the other wrapper is referenced.
   */
  public void addReference(final String aKey, final String aId) {
    if (aKey.equals(Resource.REFERENCEKEY)) {
      // check whether we still need an inverse reference for this default key
      if (hasReferenceTo(aId)) {
        // inverse reference already exists
        return;
      }
    }
    if (mReferences.get(aKey) == null) {
      // this is the first reference for this predicate key
      final Set<String> refSet = new HashSet<>();
      refSet.add(aId);
      mReferences.put(aKey, refSet);
    } else {
      // this are already references for this predicate key existing
      Set<String> refSet = mReferences.get(aKey);
      refSet.add(aId);
    }
    mLinkView = null;
    mEmbedView = null;
  }

  private boolean hasReferenceTo(String aId) {
    for (Entry<String, Set<String>> referenceEntry : mReferences.entrySet()) {
      if (referenceEntry.getValue().contains(aId)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Get the key id of this wrapper.
   * 
   * @return the key id of this wrapper.
   */
  public String getKeyId() {
    return mKeyId;
  }

  /**
   * Get all references to other wrappers.
   * 
   * @return all references to other wrappers.
   */
  public Map<String, Set<String>> getReferences() {
    return mReferences;
  }

  public void createInverseReferences(Map<String, DenormalizeResourceWrapper> aWrappedResources,
      Map<String, String> aKnownInverseRelations) {
    for (Entry<String, Set<String>> referenceSet : mReferences.entrySet()) {
      String key = referenceSet.getKey();
      for (String id : referenceSet.getValue()) {
        String inverseKey = getInverseReference(key, aKnownInverseRelations);
        aWrappedResources.get(id).addReference(inverseKey, mKeyId);
      }
    }
  }

  private static String getInverseReference(final String aKey,
      Map<String, String> aKnownInverseRelations) {
    String reverseRef = aKnownInverseRelations.get(aKey);
    return (StringUtils.isEmpty(reverseRef) ? Resource.REFERENCEKEY : reverseRef);
  }

  public void createLinkView() {
    mLinkView = Resource.getLinkClone(mResource);
  }

  public void createEmbedView(Map<String, DenormalizeResourceWrapper> aWrappedResources,
      List<String> aListValueEntries) {
    mEmbedView = Resource.getEmbedClone(mResource);
    for (Entry<String, Set<String>> referenceEntry : mReferences.entrySet()) {
      if (aListValueEntries.contains(referenceEntry.getKey())) {
        List<Resource> refList = new ArrayList<>();
        referenceEntry.getValue().forEach(id -> {
          refList.add(aWrappedResources.get(id).getLinkView());
        });
        mEmbedView.put(referenceEntry.getKey(), refList);
      } //
      else {
        referenceEntry.getValue().forEach(id -> {
          mEmbedView.put(referenceEntry.getKey(), aWrappedResources.get(id).getLinkView());
        });
      }
    }
  }

  private Resource getLinkView() {
    return mLinkView;
  }

  public Resource export(Map<String, DenormalizeResourceWrapper> aWrappedResources,
      List<String> aListValueEntries) {
    for (Entry<String, Set<String>> referenceEntry : mReferences.entrySet()) {
      if (aListValueEntries.contains(referenceEntry.getKey())) {
        List<Resource> refList = new ArrayList<>();
        referenceEntry.getValue().forEach(id -> {
          refList.add(aWrappedResources.get(id).getEmbedView());
        });
        mResource.put(referenceEntry.getKey(), refList);
      } //
      else {
        referenceEntry.getValue().forEach(id -> {
          mResource.put(referenceEntry.getKey(), aWrappedResources.get(id).getEmbedView());
        });
      }
    }
    return mResource;
  }

  private Resource getEmbedView() {
    return mEmbedView;
  }
}