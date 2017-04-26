package services.imports;

import helpers.JsonTest;
import models.Resource;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by pvb
 */
public class CalendarImporterTest implements JsonTest{

  @Test
  public void testMultipleEventsImport() throws IOException {
    List<Resource> expected =
      getResourcesFromPagedCollectionFile("CalendarImporterTest/testMultipleEventsImport.OUT.1.json").getItems();
    List<Resource> imported = CalendarImporter.importFromUrl(
      "file:///./test/resources/CalendarImporterTest/testMultipleEventsImport.IN.1.ical", "de", "");
      // TODO: fill mapzen API key (third parameter)
    assertEquals(expected, imported);
  }

}
