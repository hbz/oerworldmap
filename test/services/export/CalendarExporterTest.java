package services.export;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import helpers.JsonTest;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import models.Resource;
import models.ResourceList;
import org.junit.Test;

/**
 * Created by pvb on 14.10.16.
 */
public class CalendarExporterTest implements JsonTest {

  CalendarExporter mExporter = new CalendarExporter(Locale.ENGLISH);

  @Test
  public void testSingleEventExport() throws IOException {
    Resource singleEvent = getResourceFromJsonFile(
      "CalendarExporterTest/testSingleResourceExport.IN.1.json");
    List<String> exported = splitLines(mExporter.export(singleEvent));
    List<String> expected = splitLines(
      getStringFromFile("CalendarExporterTest/testSingleResourceExport.OUT.1.iCal",
        Charset.forName("UTF-8")));
    compare(exported, expected);
  }

  @Test
  public void testMultipleEventsExport() throws IOException {
    Resource multipleEvents1 = getResourceFromJsonFile(
      "CalendarExporterTest/testMultipleResourcesExport.IN.1.json");
    Resource multipleEvents2 = getResourceFromJsonFile(
      "CalendarExporterTest/testMultipleResourcesExport.IN.2.json");
    Resource multipleEvents3 = getResourceFromJsonFile(
      "CalendarExporterTest/testMultipleResourcesExport.IN.3.json");
    Resource multipleEvents4 = getResourceFromJsonFile(
      "CalendarExporterTest/testMultipleResourcesExport.IN.4.json");
    ResourceList multipleEvents = new ResourceList(
      Arrays.asList(multipleEvents1, multipleEvents2, multipleEvents3, multipleEvents4),
      0, null, 0, 0, null, null, null);
    List<String> exported = splitLines(mExporter.export(multipleEvents));
    List<String> expected = splitLines(
      getStringFromFile("CalendarExporterTest/testMultipleResourcesExport.OUT.1.iCal",
        Charset.forName("UTF-8")));
    compare(exported, expected);
  }

  @Test
  public void testFragmentaryResourcesListExport() throws IOException {
    ResourceList fragmentaryResources = getResourcesFromPagedCollectionFile(
      "CalendarExporterTest/testFragmentaryResourcesListExport.IN.1.json");
    List<String> exported = splitLines(mExporter.export(fragmentaryResources));
    List<String> expected = splitLines(
      getStringFromFile("CalendarExporterTest/testFragmentaryResourcesListExport.OUT.1.iCal",
        Charset.forName("UTF-8")));
    compare(exported, expected);
  }

  @Test
  public void testExportMultiLanguage() throws IOException {
    Resource resourceWithGermanDescription = getResourceFromJsonFile(
      "CalendarExporterTest/testExportMultiLanguage.IN.1.json");
    List<String> exported = splitLines(mExporter.export(resourceWithGermanDescription));
    List<String> expected = splitLines(
      getStringFromFile("CalendarExporterTest/testExportMultiLanguage.OUT.1.iCal",
        Charset.forName("UTF-8")));
    compare(exported, expected);
  }

  @Test
  public void testExportMissingRequiredFieldStartDate() throws IOException {
    Resource resourceMissingDate = getResourceFromJsonFile(
      "CalendarExporterTest/testExportMissingRequiredFieldStartDate.IN.1.json");
    List<String> exported = splitLines(mExporter.export(resourceMissingDate));
    // Export is expected to contain no events here.
    List<String> expected = splitLines(
      getStringFromFile("CalendarExporterTest/testExportMissingRequiredFieldStartDate.OUT.1.iCal",
        Charset.forName("UTF-8")));
    compare(exported, expected);
  }

  private void compare(List<String> exported, List<String> expected) {
    assertFalse("Export is too short.", expected.size() > exported.size());
    assertFalse("Export is too long.", expected.size() < exported.size());
    compareLines(exported, expected);
  }

  private void compareLines(List<String> aExported, List<String> aExpected) {
    for (String line : aExported) {
      if (line.startsWith("DTSTAMP:")) {
        assertTrue("Exported event does not contain proper time stamp: ".concat(line),
          line.matches("DTSTAMP:[0-9]{8}T[0-9]{6}Z"));
      } //
      else {
        assertTrue("Expected event does not contain following line: ".concat(line),
          aExpected.contains(line));
      }
    }
  }
}
