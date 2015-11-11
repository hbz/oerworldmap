package services.export;

import java.util.List;

import models.Resource;

public abstract class AbstractCsvExporter {

  /**
   * Extract all export columns from the given data and store internally in the
   * CsvExporter.
   * 
   * @param aResourceList
   *          a data set comprising all columns (field names / keys) that shall
   *          be exported.
   */
  public abstract void defineHeaderColumns(List<Resource> aResourceList);

  /**
   * Export the argument Resource as a CSV string.
   * 
   * @param aResource
   *          A Resource to be exported as a CSV line.
   * @return a String containing all field contents of the given Resource
   */
  public abstract String exportResourceAsCsvLine(Resource aResource);

  /**
   * Export the header line as CSV string.
   * 
   * @return a String containing all columns, resp. field names of the data set
   */
  public abstract String headerKeysToCsvString();

}
