package services.export;

import models.Resource;
import models.ResourceList;

/**
 * Created by fo on 13.10.16.
 */
public interface Exporter {

  String export(Resource aResource);

  String export(ResourceList aResourceList);
}
