package services.export;

import static org.junit.Assert.assertEquals;

import helpers.JsonTest;
import io.apigee.trireme.kernel.Charsets;
import models.Resource;
import models.ResourceList;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

public class CsvExporterTest implements JsonTest {

  private final static CsvExporter mCsvExporter = new CsvExporter();

  @Test
  public void testSingleExport() throws IOException {
    Resource in1 = getResourceFromJsonFile("CsvExporterTest/testPlainExport.IN.1.json");
    String expected = getStringFromFile("CsvExporterTest/testPlainExport.OUT.1.csv", Charsets.UTF8);
    assertEquals(expected, mCsvExporter.export(in1));
  }

  @Test
  public void testListExport() throws IOException {
    Resource in1 = getResourceFromJsonFile("CsvExporterTest/testPlainExport.IN.1.json");
    Resource in2 = getResourceFromJsonFile("CsvExporterTest/testPlainExport.IN.2.json");
    ResourceList resourceList = new ResourceList(Arrays.asList(in1, in2), 2, null, 0, 2, null, null, null);
    String expected = getStringFromFile("CsvExporterTest/testPlainExport.OUT.2.csv", Charsets.UTF8);
    assertEquals(expected, mCsvExporter.export(resourceList));
  }

}
