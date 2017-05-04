package services.imports;

import helpers.JsonTest;
import helpers.ResourceHelpers;
import models.Resource;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by pvb
 */
public class CalendarImporterTest implements JsonTest{

  @Test
  public void testMultipleEventsImport() throws IOException {
    List<Resource> expected = ResourceHelpers.getResourcesWithoutIds(ResourceHelpers.unwrapRecords(
      getResourcesFromPagedCollectionFile("CalendarImporterTest/testMultipleEventsImport.OUT.1.json").getItems()));
    List<Resource> imported = ResourceHelpers.getResourcesWithoutIds(CalendarImporter.importFromUrl(
      "file://" + new File(".").getAbsolutePath() +
      "/test/resources/CalendarImporterTest/testMultipleEventsImport.IN.1.ical", "de", "<api key goes here>"));
      // TODO: fill mapzen API key (third parameter)
    Collections.sort(expected);
    Collections.sort(imported);
    assertEquals(expected.toString().trim(), imported.toString().trim());
  }

}
