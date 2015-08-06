package services;

import helpers.JsonLdConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import models.Resource;

import com.rits.cloning.Cloner;

public class ResourceTrimmer {

  private ResourceTrimmer() { /* no instantiation */
  }

  /**
   * Returns a truncated clone of the given Resource.
   */
  public static Resource trim(Resource aResource) throws IOException {
    return trimClone(aResource, null);
  }
  
  /**
   * Returns a truncated clone of the given Resource that is enriched
   * with information contained in the given ResourceRepository.
   */
  public static Resource trim(Resource aResource, ResourceRepository aRepo) throws IOException {
    return trimClone(aResource, aRepo);
  }

  private static Resource trimClone(Resource aResource, ResourceRepository aRepo) throws IOException {
    Resource result = null;
    if (aRepo != null) {
      result = aRepo.getResource(aResource.getAsString(JsonLdConstants.ID));
    }
    if (result == null || result.isEmpty()) {
      result = new Resource();
    }
    Resource clone = new Cloner().deepClone(aResource);
    truncateResource(clone);
    for (Entry<String, Object> entry : clone.entrySet()) {
      result.put(entry.getKey(), entry.getValue());
    }
    return result;
  }

  private static void truncateResource(Resource aResource) {
    for (Iterator<Map.Entry<String, Object>> it = aResource.entrySet().iterator(); it.hasNext();) {
      Map.Entry<String, Object> entry = it.next();
      // truncate subresources
      if (entry.getValue() instanceof Resource) {
        aResource.put(entry.getKey(), Resource.getEmbedClone((Resource) entry.getValue()));
      }
      // truncate lists of subresources
      else if (entry.getValue() instanceof List) {
        List<?> list = (List<?>) (entry.getValue());
        List<Object> truncatedList = new ArrayList<>();
        for (Iterator<?> innerIt = list.iterator(); innerIt.hasNext();) {
          Object li = innerIt.next();
          if (li instanceof Resource) {
            truncatedList.add(Resource.getEmbedClone((Resource) li));
          } else {
            truncatedList.add(li);
          }
          if (truncatedList.isEmpty()) {
            it.remove();
          }
        }
        aResource.put(entry.getKey(), truncatedList);
      }
    }
  }
  
}