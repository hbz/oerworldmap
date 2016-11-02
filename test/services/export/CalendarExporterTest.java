package services.export;

import helpers.FileHelpers;
import helpers.JsonTest;
import models.Resource;
import models.ResourceList;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertTrue;

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
    multipleEvents = new ResourceList(Arrays.asList(multipleEvents1, multipleEvents2, multipleEvents3), 0, null, 0, 0, null, null, null);
  }

  @Test
  public void testSingleEventExport() throws IOException {
    List<String> singleEventExported = splitLines(mExporter.export(singleEvent));
    BufferedReader reader = FileHelpers.getBufferedReaderFrom("CalendarExporterTest/testSingleResourceExport.OUT.1.iCal");
    String line = reader.readLine();
    while (line != null && !line.isEmpty()){
      assertTrue("Exported event does not contain following line: ".concat(line), singleEventExported.contains(line));
      line = reader.readLine();
    }
  }

  // @Test
  public void testMultipleEventsExport() throws IOException {
    List<String> multipleEventsExported = splitLines(mExporter.export(multipleEvents));
    BufferedReader reader = FileHelpers.getBufferedReaderFrom("CalendarExporterTest/testMultipleResourcesExport.OUT.1.json");
    String line = reader.readLine();
    while (line != null && !line.isEmpty()){
      assertTrue("Exported events do not contain following line: ".concat(line), multipleEventsExported.contains(line));
      line = reader.readLine();
    }
  }

  private static List<String> splitLines(String aString){
    return Arrays.asList(aString.split("\n"));
  }
}
