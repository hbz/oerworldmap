package services.export;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import helpers.JsonTest;
import models.Resource;

public class CsvExporterTest implements JsonTest {

  private final static CsvExporter mCsvExporter = new CsvExporter();
  private Resource in1;
  private Resource in2;

  @Before
  public void setup() throws IOException {
    in1 = getResourceFromJsonFile("CsvExporterTest/testPlainExport.IN.1.json");
    in2 = getResourceFromJsonFile("CsvExporterTest/testPlainExport.IN.2.json");
    List<Resource> mockSearchResultItems = new ArrayList<>();
    mockSearchResultItems.add(in1);
    mockSearchResultItems.add(in2);
    mCsvExporter.defineHeaderColumns(mockSearchResultItems);
  }

  @Test
  public void testHeader() throws IOException {
    assertEquals("@id;@type;authorOf;email;name", mCsvExporter.headerKeysToCsvString());
  }

  @Test
  public void testPlainExport() throws IOException {
    String csv1 = mCsvExporter.exportResourceAsCsvLine(in1);
    String csv2 = mCsvExporter.exportResourceAsCsvLine(in2);
    assertEquals("456;Person;123;null;Hans Dampf", csv1);
    assertEquals("345;Person;123;foo@bar.com;Hans Wurst", csv2);
  }

}
