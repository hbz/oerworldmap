package services.export;

import helpers.JsonTest;
import models.ModelCommon;
import models.Resource;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CsvWithNestedIdsExporterTest implements JsonTest {

  private final static CsvWithNestedIdsExporter mCsvExporter = new CsvWithNestedIdsExporter();
  private Resource in1;
  private Resource in2;

  @Before
  public void setup() throws IOException {
    in1 = getResourceFromJsonFile("CsvWithNestedIdsExporterTest/testPlainExport.IN.1.json");
    in2 = getResourceFromJsonFile("CsvWithNestedIdsExporterTest/testPlainExport.IN.2.json");
    List<ModelCommon> mockSearchResultItems = new ArrayList<>();
    mockSearchResultItems.add(in1);
    mockSearchResultItems.add(in2);
    mCsvExporter.defineHeaderColumns(mockSearchResultItems);
  }

  @Test
  public void testHeader() throws IOException {
    assertEquals("@id;@type;address;authorOf;email;name", mCsvExporter.headerKeysToCsvString());
  }

  // FIXME: Wrap resources in records
  //@Test
  public void testPlainExport() throws IOException {
    String csv1 = mCsvExporter.export(in1);
    String csv2 = mCsvExporter.export(in2);
    assertEquals("@id;@type;address;authorOf;name\n" +
        "456;Person;Countrycountry, Streetstreet 1, 123456;123, 987;Hans Dampf\n", csv1);
    assertEquals("@id;@type;authorOf;email;name\n" +
      "345;Person;123;foo@bar.com;Hans Wurst\n", csv2);
  }

}
