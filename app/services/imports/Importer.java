package services.imports;

import models.Resource;

import java.io.IOException;
import java.util.List;

/**
 * Created by pvb
 */
public interface Importer {

    List<Resource> importFromUrl(String aUrl, String aLanguage) throws IOException;

}
