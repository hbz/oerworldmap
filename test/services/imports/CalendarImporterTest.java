package services.imports;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import helpers.JsonTest;
import helpers.ResourceHelpers;
import models.Resource;
import org.junit.BeforeClass;
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

  private static Config mConfig;
  private static CalendarImporter calendarImporter;

  @BeforeClass
  public static void setupResources() throws IOException {
    mConfig = ConfigFactory.parseFile(new File("conf/test.conf")).resolve();
    calendarImporter = new CalendarImporter(mConfig);
  }

  @Test
  public void testMultipleEventsImport() throws IOException {
    List<Resource> expected = ResourceHelpers.getResourcesWithoutIds(ResourceHelpers.unwrapRecords(
      getResourcesFromPagedCollectionFile("CalendarImporterTest/testMultipleEventsImport.OUT.1.json").getItems()));
    List<Resource> imported = ResourceHelpers.getResourcesWithoutIds(calendarImporter.importFromUrl(
      "file://" + new File(".").getAbsolutePath() +
      "/test/resources/CalendarImporterTest/testMultipleEventsImport.IN.1.ical", "de"));
    Collections.sort(expected);
    Collections.sort(imported);
    assertEquals(expected.toString().trim(), imported.toString().trim());
  }

}
