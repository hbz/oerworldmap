package services.imports;

import helpers.JsonTest;
import models.Resource;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by pvb
 */
public class CalendarImporterTest implements JsonTest{

  @Test
  public void testMultipleEventsImport() throws IOException {
    List<Resource> expected = new ArrayList<>();
    expected.add(getResourceFromJsonFile("CalendarImporterTest/testMultipleEventsImport.IN.1.json"));
    List<Resource> imported = CalendarImporter.importFromUrl(
      "file:///./test/resources/CalendarImporterTest/testMultipleEventsImport.IN.1.iCal", "de", "");
      // TODO: fill mapzen API key (third parameter)
    assertEquals(expected, imported);
  }

}
