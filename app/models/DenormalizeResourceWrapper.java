package models;

import helpers.JsonLdConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

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
      getMentionedResourcesFromRepo(aWrappedResources, mResource, aRepo, 2);
    }
    addResource(aResource);
    mLinkView = null;
    mEmbedView = null;
  }

  private DenormalizeResourceWrapper(final Resource aResource,
      final Map<String, DenormalizeResourceWrapper> aWrappedResources,
      final ResourceRepository aRepo, final int aSubLevels) throws IOException {
    mReferences = new HashMap<>();
    mKeyId = aResource.getAsString(JsonLdConstants.ID);
    mResource = aRepo.getResource(mKeyId);
    getMentionedResourcesFromRepo(aWrappedResources, mResource, aRepo, aSubLevels);
    mLinkView = null;
    mEmbedView = null;
  }

  /*
   * 
   */
  private static void getMentionedResourcesFromRepo(
      final Map<String, DenormalizeResourceWrapper> aWrappedResources, final Resource aResource,
      final ResourceRepository aRepo, final int aSubLevels) throws IOException {
    for (Entry<String, Object> entry : aResource.entrySet()) {
      if (entry.getValue() instanceof Resource) {
        putResourceToWrapperList(aWrappedResources, aRepo, aSubLevels,
            ((Resource) entry.getValue()));
      } //
      else if (entry.getValue() instanceof List) {
        Iterator<?> iter = ((List<?>) entry.getValue()).iterator();
        while (iter.hasNext()) {
          Object next = iter.next();
          if (next instanceof Resource) {
            putResourceToWrapperList(aWrappedResources, aRepo, aSubLevels, (Resource) next);
          }
        }
      }
    }
  }

  /*
   * 
   */
  private static void putResourceToWrapperList(
      final Map<String, DenormalizeResourceWrapper> aWrappedResources,
      final ResourceRepository aRepo, final int aSubLevels, Resource resource) throws IOException {
    String id = resource.getAsString(JsonLdConstants.ID);
    if (!aWrappedResources.containsKey(id)) {
      aWrappedResources.put(id, new DenormalizeResourceWrapper(resource, aWrappedResources, aRepo,
          aSubLevels - 1));
    }
  }

  /**
   * TODO
   * 
   * @param aResource
   */
  public void addResource(Resource aResource) {
    extractFirstLevelReferences(aResource);
    mResource.merge(Resource.getFlatClone(aResource));
    mLinkView = null;
    mEmbedView = null;
  }

  /*
   * 
   */
  private void extractFirstLevelReferences(Resource aResource) {
    for (final Entry<String, Object> entry : aResource.entrySet()) {
      if (entry.getValue() instanceof Resource) {
        addReference(entry.getKey(), ((Resource) entry.getValue()).getAsString(JsonLdConstants.ID));
      }
      if (entry.getValue() instanceof List) {
        for (final Object listItem : (List<?>) entry.getValue()) {
          if (listItem instanceof Resource) {
            addReference(entry.getKey(), ((Resource) listItem).getAsString(JsonLdConstants.ID));
          }
        }
      }
    }
  }

  /**
   * Adds additional reference data to another wrapper, providing the key by
   * which the other wrapper is referenced.
   */
  public void addReference(final String aKey, final String aId) {
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
      referenceSet.getValue().forEach(id -> {
        String inverseKey = getInverseReference(key, aKnownInverseRelations);
        aWrappedResources.get(id).addReference(inverseKey, mKeyId);
      });
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