package services.export;

import helpers.JsonTest;
import models.Resource;
import models.ResourceList;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Created by pvb on 14.10.16.
 */
public class CalendarExporterTest implements JsonTest {

  CalendarExporter mExporter = new CalendarExporter(Locale.ENGLISH);
  Resource singleEvent;
  ResourceList multipleEvents;

  @Before
  public void setup() throws IOException {
    singleEvent = getResourceFromJsonFile("CalendarExporterTest/testSingleResourceExport.IN.1.json");
    Resource multipleEvents1 = getResourceFromJsonFile("CalendarExporterTest/testMultipleResourcesExport.IN.1.json");
    Resource multipleEvents2 = getResourceFromJsonFile("CalendarExporterTest/testMultipleResourcesExport.IN.2.json");
    Resource multipleEvents3 = getResourceFromJsonFile("CalendarExporterTest/testMultipleResourcesExport.IN.3.json");
    Resource multipleEvents4 = getResourceFromJsonFile("CalendarExporterTest/testMultipleResourcesExport.IN.4.json");
    multipleEvents = new ResourceList(Arrays.asList(multipleEvents1, multipleEvents2, multipleEvents3, multipleEvents4),
      0, null, 0, 0, null, null, null);
  }

  @Test
  public void testSingleEventExport() throws IOException {
    List<String> exported = splitLines(mExporter.export(singleEvent));
    List<String> expected = splitLines(getStringFromFile("CalendarExporterTest/testSingleResourceExport.OUT.1.iCal",
      Charset.forName("UTF-8")));
    assertFalse("Export is too short.", expected.size() > exported.size());
    assertFalse("Export is too long.", expected.size() < exported.size());
    compareLines(exported, expected);
  }

  @Test
  public void testMultipleEventsExport() throws IOException {
    List<String> exported = splitLines(mExporter.export(multipleEvents));
    List<String> expected = splitLines(getStringFromFile("CalendarExporterTest/testMultipleResourcesExport.OUT.1.iCal",
      Charset.forName("UTF-8")));
    assertFalse("Export is too short.", expected.size() > exported.size());
    assertFalse("Export is too long.", expected.size() < exported.size());
    compareLines(exported, expected);
  }

  private static List<String> splitLines(String aString){
    return Arrays.asList(aString.split("\n"));
  }

  private void compareLines(List<String> aExported, List<String> aExpected) {
    for (String line : aExported){
      if (line.startsWith("DTSTAMP:")){
        assertTrue("Exported event does not contain proper time stamp: ".concat(line), line.matches("DTSTAMP:[0-9]{8}T[0-9]{6}Z"));
      } //
      else {
        assertTrue("Expected event does not contain following line: ".concat(line), aExpected.contains(line));
      }
    }
  }


}
