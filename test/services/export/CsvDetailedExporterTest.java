package services.export;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import helpers.JsonTest;
import models.Resource;

public class CsvDetailedExporterTest implements JsonTest {

  private final static AbstractCsvExporter mCsvExporter = new CsvDetailedExporter();
  private Resource in1;
  private Resource in2;

  @Before
  public void setup() throws IOException {
    in1 = getResourceFromJsonFile("CsvDetailedExporterTest/testPlainExport.IN.1.json");
    in2 = getResourceFromJsonFile("CsvDetailedExporterTest/testPlainExport.IN.2.json");
    List<Resource> mockSearchResultItems = new ArrayList<>();
    mockSearchResultItems.add(in1);
    mockSearchResultItems.add(in2);
    mCsvExporter.defineHeaderColumns(mockSearchResultItems);
  }

  @Test
  public void testHeader() throws IOException {
    assertEquals(
        "@id;@type;authorOf>0>@id;authorOf>0>@type;authorOf>0>articleBody;authorOf>0>author>0>@id;authorOf>0>author>0>@type;authorOf>0>author>0>name;authorOf>0>author>1>@id;authorOf>0>author>1>@type;authorOf>0>author>1>name;authorOf>0>name;authorOf>1>@id;authorOf>1>@type;authorOf>1>author>0>@id;authorOf>1>author>0>@type;authorOf>1>author>0>name;authorOf>1>name;email;name;",
        mCsvExporter.headerKeysToCsvString());
  }

  @Test
  public void testPlainExport() throws IOException {
    String csv1 = mCsvExporter.export(in1);
    String csv2 = mCsvExporter.export(in2);
    assertEquals(
        "456;Person;123;Article;Super toll;456;Person;Hans Dampf;null;null;null;Ganz spannend!;987;Article;456;Person;Hans Dampf;Noch spannenderen!;null;Hans Dampf",
        csv1);
    assertEquals(
        "345;Person;123;Article;Super toll;456;Person;Hans Dampf;345;Person;Hans Wurst;Ganz spannend!;null;null;null;null;null;null;foo@bar.com;Hans Wurst",
        csv2);
  }

}
