package services.export;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import helpers.JsonTest;
import models.Resource;

public class CsvWithNestedIdsExporterTest implements JsonTest {

  private final static CsvWithNestedIdsExporter mCsvExporter = new CsvWithNestedIdsExporter();
  private Resource in1;
  private Resource in2;

  @Before
  public void setup() throws IOException {
    in1 = getResourceFromJsonFile("CsvWithNestedIdsExporterTest/testPlainExport.IN.1.json");
    in2 = getResourceFromJsonFile("CsvWithNestedIdsExporterTest/testPlainExport.IN.2.json");
    List<Resource> mockSearchResultItems = new ArrayList<>();
    mockSearchResultItems.add(in1);
    mockSearchResultItems.add(in2);
    mCsvExporter.defineHeaderColumns(mockSearchResultItems);
  }

  @Test
  public void testHeader() throws IOException {
    assertEquals("@id;@type;address;authorOf;email;name", mCsvExporter.headerKeysToCsvString());
  }

  @Test
  public void testPlainExport() throws IOException {
    String csv1 = mCsvExporter.export(in1);
    String csv2 = mCsvExporter.export(in2);
    assertEquals("456;Person;Countrycountry, Streetstreet 1, Place, 123456;123;null;Hans Dampf",
        csv1);
    assertEquals("345;Person;null;123;foo@bar.com;Hans Wurst", csv2);
  }

}
