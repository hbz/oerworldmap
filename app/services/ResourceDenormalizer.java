package services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Resource;

import org.apache.commons.lang3.StringUtils;

public class ResourceDenormalizer {

  final private static Map<String, String> mKnownRelations = new HashMap<>();

  // TODO: add configurable member variables / parameters that specify in what
  // manner exactly Resources should be denormalized ??

  public ResourceDenormalizer() {
    initializeRelations();
  }

  public static List<Resource> denormalize(Resource aResource, BaseRepository aRepo) {

    // create flat list of the given resource and all of its subresources
    List<Resource> result = new ArrayList<>();
    splitDescendantResources(result, aResource);

    // ensure that data granulation level is appropriate
    result.forEach(r -> {
      Resource trimmedResource;
      try {
        trimmedResource = ResourceTrimmer.trim(r, aRepo);
        result.remove(r);
        result.add(trimmedResource);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
    return result;
  }

  private static List<Resource> splitDescendantResources(List<Resource> aList, Resource aResource) {
    aList.add(aResource);
    for (Map.Entry<String, Object> entry : aResource.entrySet()) {
      Object value = entry.getValue();
      // TODO: check for directly nested objects like {"about" : {"author" :
      // "John Doe"}}
      if (value instanceof Resource) {
        Resource res = (Resource) value;
        splitDescendantResources(aList, res);
        res.put(getReverseReference(entry.getKey()), aResource);
      } else if (value instanceof List) {
        for (Object listItem : (List<?>) value) {
          if (listItem instanceof Resource) {
            Resource res = (Resource) value;
            splitDescendantResources(aList, res);
            res.put(getReverseReference(entry.getKey()), aResource);
          }
        }
      }
    }
    return aList;
  }

  private static String getReverseReference(String aKey) {
    String reverseRef = mKnownRelations.get(aKey);
    return (StringUtils.isEmpty(reverseRef) ? "referencedBy" : reverseRef);
  }

  private void initializeRelations() {
    mKnownRelations.put("author", "authorOf");
    mKnownRelations.put("authorOf", "author");
  }
}
