package services.export;

import java.util.List;

import controllers.OERWorldMap;
import models.Resource;

public class BaseExporter extends OERWorldMap {

  // TODO: store in File
  public void exportQueryAsCsv(String aQuery, int aFrom, int aSize, String aSortOrder,
      boolean aDetailedCsv) {

    StringBuffer result = new StringBuffer();
    AbstractCsvExporter csvExporter;
    if (aDetailedCsv) {
      csvExporter = new CsvDetailedExporter();
    } else {
      csvExporter = new CsvWithNestedIdsExporter();
    }

    List<Resource> queryResult = mBaseRepository.query(aQuery, aFrom, aSize, aSortOrder, null).getItems();
    csvExporter.defineHeaderColumns(queryResult);
    result.append(csvExporter.headerKeysToCsvString());

    for (Resource resource : queryResult) {
      csvExporter.exportResourceAsCsvLine(resource);
    }
  }

}
