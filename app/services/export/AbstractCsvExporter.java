package services.export;

import java.util.List;

import models.Resource;
import models.ResourceList;

public abstract class AbstractCsvExporter implements Exporter{

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
   * Specify which information is to be dropped from the CSV export by a List of
   * field names.
   *
   * @param aDropFields
   *          a List<String> containing fields names to be dropped from the
   *          export. This is useful especially for static information like JSON
   *          "type".
   */
  public abstract void setDropFields(List<String> aDropFields);

  /**
   * Export the argument Resource as a CSV string.
   *
   * @param aResource
   *          A Resource to be exported as a CSV line.
   * @return a String containing all field contents of the given Resource
   */
  @Override
  public abstract String export(Resource aResource);

  /**
   * Export the argument ResourceList as CSV rows.
   * @param aResourceList
   *          A ResourceList to be exported as a CSV line.
   * @return a String containing all field contents of the given ResourceList including the CSV header
     */
  @Override
  public String export(ResourceList aResourceList) {
    // currently not implemented
    return null;
  }

  /**
   * Export the header line as CSV string.
   *
   * @return a String containing all columns, resp. field names of the data set
   */
  public abstract String headerKeysToCsvString();

}
