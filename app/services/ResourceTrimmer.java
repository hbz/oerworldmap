package services;

import helpers.JsonLdConstants;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import models.Resource;

import com.rits.cloning.Cloner;

public class ResourceTrimmer {

  private ResourceTrimmer() { /* no instantiation */
  }

  public static Resource trim(Resource aResource, BaseRepository aRepo) {
    return trimClone(aResource, aRepo);
  }

  private static Resource trimClone(Resource aResource,
      BaseRepository aRepo) {
    Resource result = aRepo.getResource(aResource.get(JsonLdConstants.ID).toString());
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
      if (entry.getValue() instanceof Resource){
        aResource.put(entry.getKey(), Resource.getEmbedView((Resource)entry.getValue(), false));
      }
    }
  }
}
