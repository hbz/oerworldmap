package services.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import helpers.JsonTest;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import models.Resource;
import models.ResourceList;
import org.junit.Before;
import org.junit.Test;

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
  public void testHeader() {
    assertEquals(
      "@id;@type;authorOf>0>@id;authorOf>0>@type;authorOf>0>articleBody;authorOf>0>author>0>@id;authorOf>0>author>0>@type;authorOf>0>author>0>name;authorOf>0>author>1>@id;authorOf>0>author>1>@type;authorOf>0>author>1>name;authorOf>0>name;authorOf>1>@id;authorOf>1>@type;authorOf>1>author>0>@id;authorOf>1>author>0>@type;authorOf>1>author>0>name;authorOf>1>name;email;name;",
      mCsvExporter.headerKeysToCsvString());
  }

  @Test
  public void testPlainExport() {
    String csv1 = mCsvExporter.export(in1);
    String csv2 = mCsvExporter.export(in2);
    assertEquals(
      "456;\"Person\";123;\"Article\";\"Super toll\";456;\"Person\";\"Hans Dampf\";;;;\"Ganz spannend!\";987;\"Article\";456;\"Person\";\"Hans Dampf\";\"Noch spannenderen!\";;\"Hans Dampf\"",
      csv1);
    assertEquals(
      "345;\"Person\";123;\"Article\";\"Super toll\";456;\"Person\";\"Hans Dampf\";345;\"Person\";\"Hans Wurst\";\"Ganz spannend!\";;;;;;;\"foo@bar.com\";\"Hans Wurst\"",
      csv2);
  }

  @Test
  public void testEscapedStringExport() throws IOException {
    AbstractCsvExporter csvExporter = new CsvDetailedExporter();
    Resource resource = getResourceFromJsonFile(
      "CsvDetailedExporterTest/testEscapedStringExport.IN.1.json");
    List<Resource> asList = new ArrayList<>();
    asList.add(resource);
    csvExporter.defineHeaderColumns(asList);
    String csv = csvExporter.export(resource);
    assertEquals(
      "456;\"Person\";\"Hans is a very sophisticated person, one you would need delimiters to tell his abilities; real delimiters| I mean: many of them. Plenty.\";\"Hans Dampf\"",
      csv);
  }

  @Test
  public void testExportResourceList() throws IOException {
    ResourceList multipleEvents = new ResourceList(Arrays.asList(in1, in2),
      0, null, 0, 0, null, null, null);
    List<String> exported = splitLines(mCsvExporter.export(multipleEvents));
    List<String> expected = splitLines(
      getStringFromFile("CsvDetailedExporterTest/testExportResourceList.OUT.1.csv",
        Charset.forName("UTF-8")));
    assertFalse("Export is too short.", expected.size() > exported.size());
    assertFalse("Export is too long.", expected.size() < exported.size());
    compareLines(exported, expected);
  }

  private void compareLines(List<String> aExported, List<String> aExpected) {
    for (String line : aExported) {
      assertTrue("The CSV export line was not expected: ".concat(line), aExpected.contains(line));
    }
  }
}
