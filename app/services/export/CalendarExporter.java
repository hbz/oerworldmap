package services.export;

import helpers.JsonLdConstants;
import models.Resource;
import models.ResourceList;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by fo and pvb
 */
public class CalendarExporter implements Exporter {

  // The following iCalendar fields are currently not in use:
  // - SEQUENCE
  // - STATUS
  // - TRANSP
  // - RRULE
  // - CATEGORIES

  private static final String HEADER = //
    "BEGIN:VCALENDAR\n" + //
    "VERSION:2.0\n" + //
    "PRODID:https://oerworldmap.org/\n" + //
    "CALSCALE:GREGORIAN\n" + //
    "METHOD:PUBLISH\n";
  private static final String FOOTER = "END:VCALENDAR\n";
  private static final String SEPARATOR = ":";

  private static final String EVENT_BEGIN = "BEGIN:VEVENT\n";
  private static final String EVENT_END = "END:VEVENT\n";

  private static final String UID = "UID:";
  private static final String ORGANIZER = "ORGANIZER:";
  private static final String CLEAR_NAME = "CN=";       // in line ORGANIZER
  private static final String MAILTO = "MAILTO:";       // in line ORGANIZER
  private static final String LOCATION = "LOCATION:";
  private static final String SUMMARY = "SUMMARY:";
  private static final String DESCRIPTION = "DESCRIPTION:";
  private static final String CAL_CLASS = "CLASS:PUBLIC\n";   // no non-public events up to now
  private static final String GEO = "GEO:";
  private static final String URL = "URL:";

  private static final String DATE_START = "DTSTART:";  // TODO format: 20060910T220000Z
  private static final String DATE_END = "DTEND:";      // TODO format: 20060919T215900Z
  private static final String DATE_STAMP = "DTSTAMP:";  // TODO format: 20060812T125900Z

  private static final Map<String, String> mFieldMap = new HashMap<>();
  static{
    mFieldMap.put(UID, JsonLdConstants.ID);
    mFieldMap.put(CLEAR_NAME, "organizer.name");
    mFieldMap.put(SUMMARY, "name");
    mFieldMap.put(DESCRIPTION, "description");
    mFieldMap.put(URL, "url");
    mFieldMap.put(DATE_START, "startDate");
    mFieldMap.put(DATE_END, "endDate");
    mFieldMap.put(LOCATION, "location.address");
    mFieldMap.put(GEO, "location.geo");
  }

  @Override
  public String export(Resource aResource) {
    if (!aResource.getType().equals("Event")) {
      return null;
    }
    StringBuilder result = new StringBuilder(HEADER);
    result.append(exportResourceWithoutHeader(aResource));
    result.append(FOOTER);
    return result.toString();
  }

  @Override
  public String export(ResourceList aResourceList) {
    StringBuilder result = new StringBuilder(HEADER);
    for (Resource resource : aResourceList.getItems()){
      if (resource.getType().equals("Event")){
        result.append(exportResourceWithoutHeader(resource));
      }
    }
    result.append(FOOTER);
    return null;
  }

  private String exportResourceWithoutHeader(Resource aResource){
    StringBuilder result = new StringBuilder(EVENT_BEGIN);
    for (Map.Entry<String, String> mapping : mFieldMap.entrySet()){
      String value = aResource.get(mapping.getValue()).toString();
      if (value != null){
        result.append(mapping.getKey()).append(value).append("\n");
      }
    }
    result.append(getExportedOrganizer(aResource));
    result.append(CAL_CLASS);
    result.append(DATE_STAMP).append(System.currentTimeMillis()).append("\n"); // TODO change time stamp format
    result.append(EVENT_END);
    return result.toString();
  }

  private String getExportedOrganizer(Resource aResource){
    StringBuilder result = new StringBuilder();
    Resource organizer = aResource.getAsResource("organizer");
    if (organizer != null){
      result.append(ORGANIZER);
      String name = organizer.get("name").toString();
      boolean hasName = false;
      if (name != null){
        result.append(CLEAR_NAME).append(name);
        hasName = true;
      }
      String email = aResource.get("email").toString();
      if (email == null){
        email = organizer.get("email").toString();
      }
      if (email != null){
        if (hasName){
          result.append(SEPARATOR);
        }
        result.append(MAILTO).append(email);
      }
      result.append("\n");
    }
    return result.toString();
  }
}
