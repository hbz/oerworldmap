package helpers;

import models.Record;
import models.Resource;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by boeselager on 11.01.17.
 */
public class ResourceHelpers {

  public static List<Resource> unwrapRecords(List<Resource> aRecords) {
    List<Resource> resources = new ArrayList<>();
    for (Resource rec : aRecords) {
      resources.add(unwrapRecord(rec));
    }
    return resources;
  }

  public static Resource unwrapRecord(Resource aRecord) {
    return aRecord.getAsResource(Record.RESOURCE_KEY);
  }

  public static List<Resource> getResourcesWithoutIds(List<Resource> aResourceList) {
    List<Resource> result = new ArrayList<>();
    for (Resource resource : aResourceList) {
      Resource newResource = (Resource) resource.clone();
      newResource.remove(JsonLdConstants.ID);
      result.add(newResource);
    }
    return result;
  }
}
