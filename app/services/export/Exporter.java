package services.export;

import models.ModelCommon;
import models.ModelCommonList;

/**
 * Created by fo on 13.10.16.
 */
public interface Exporter {

  String export(ModelCommon aResource);

  String export(ModelCommonList aResourceList);

}
