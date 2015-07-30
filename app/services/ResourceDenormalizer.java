package services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Resource;

import org.apache.commons.lang3.StringUtils;

public class ResourceDenormalizer {

  final private static Map<String, String> mKnownInverseRelations = new HashMap<>();
  static{
    mKnownInverseRelations.put("author", "authorOf");
    mKnownInverseRelations.put("authorOf", "author");
  }
  final private static List<String> mListValueEntries = new ArrayList<>();
  static{
    mListValueEntries.add("author");
    mListValueEntries.add("authorOf");
  }

  // TODO: add configurable member variables / parameters that specify in what
  // manner exactly Resources should be denormalized ??

  public static List<Resource> denormalize(Resource aResource, ResourceRepository aRepo) {

    // create flat list of the given resource and all of its subresources
    List<Resource> flatted = new ArrayList<>();
    List<Resource> trimmed = new ArrayList<>();
    splitDescendantResources(flatted, aResource);

    // trim the flatted resources to the appropriate granulation level
    flatted.forEach(r -> {
        try {
          trimmed.add(ResourceTrimmer.trim(r, aRepo));
        } catch (Exception e) {
          e.printStackTrace();
        }
    });
    return trimmed;
  }

  private static List<Resource> splitDescendantResources(final List<Resource> aList, final Resource aResource) {
    aList.add(aResource);
    for (final Map.Entry<String, Object> entry : aResource.entrySet()) {
      final Object value = entry.getValue();
      // TODO: check for directly nested objects like {"about" : {"author" :
      // "John Doe"}}
      if (value instanceof Resource) {
        final Resource res = (Resource) value;
        splitDescendantResources(aList, res);
        addResourceEntry(aResource, entry.getKey(), res);
      } else if (value instanceof List) {
        for (final Object listItem : (List<?>) value) {
          if (listItem instanceof Resource) {
            final Resource res = (Resource) listItem;
            splitDescendantResources(aList, res);
            addResourceEntry(aResource, entry.getKey(), res);
          }
        }
      }
    }
    return aList;
  }

  @SuppressWarnings("unchecked")
  private static void addResourceEntry(final Resource aResource, final String aKey,
      final Resource res) {
    if (mListValueEntries.contains(aKey)){
      // add the inverse reference as a list entry
      Object value = res.get(aKey);
      final List<Resource> newEntry;
      if (value == null){
        newEntry = new ArrayList<>();
      }
      else if (value instanceof List<?>){
        newEntry = (List<Resource>) value;
      }
      else{
        throw new IllegalArgumentException("Malformed entry in Resource " + res + ". Excpected a list entry for key \"" + aKey + "\".");
      }
      newEntry.add(aResource);
      res.put(getInverseReference(aKey), newEntry);
    }
    else{
      // inverse reference can be added without list wrapper
      res.put(getInverseReference(aKey), aResource);
    }
  }

  private static String getInverseReference(final String aKey) {
    String reverseRef = mKnownInverseRelations.get(aKey);
    return (StringUtils.isEmpty(reverseRef) ? "referencedBy" : reverseRef);
  }

}
