package helpers;

import models.ModelCommon;
import models.Record;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by boeselager on 11.01.17.
 */
public class ResourceHelpers {

  public static List<ModelCommon> unwrapRecords(List<ModelCommon> aRecords) {
    List<ModelCommon> resources = new ArrayList<>();
    for (ModelCommon rec : aRecords) {
      resources.add(unwrapRecord(rec));
    }
    return resources;
  }

  public static ModelCommon unwrapRecord(ModelCommon aRecord) {
    return aRecord.getAsItem(Record.RESOURCE_KEY);
  }

}
