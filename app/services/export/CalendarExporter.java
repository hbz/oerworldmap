package services.export;

import helpers.JsonLdConstants;
import models.Resource;
import models.ResourceList;
import org.elasticsearch.common.Strings;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
  private static final String KEY_SEPARATOR = ":";
  private static final String VALUE_SEPARATOR = ";";

  private static final String EVENT_BEGIN = "BEGIN:VEVENT\n";
  private static final String EVENT_END = "END:VEVENT\n";

  private static final String UID = "UID:";
  private static final String ORGANIZER = "ORGANIZER:";
  private static final String COMMON_NAME = "CN=";      // in line ORGANIZER
  private static final String MAILTO = "MAILTO:";       // in line ORGANIZER
  private static final String LOCATION = "LOCATION:";
  private static final String SUMMARY = "SUMMARY:";
  private static final String DESCRIPTION = "DESCRIPTION:";
  private static final String CAL_CLASS = "CLASS:PUBLIC\n";   // no non-public events up to now
  private static final String GEO = "GEO:";
  private static final String URL = "URL:";

  private static final String DATE_START = "DTSTART:";
  private static final String DATE_END = "DTEND:";
  private static final String DATE_STAMP = "DTSTAMP:";

  private static final String DATE_REGEX = "([\\d]{4})-([\\d]{2})-([\\d]{2})";
  private static final Pattern mDatePattern = Pattern.compile(DATE_REGEX);

  private static final Map<String, String> mFieldMap = new HashMap<>();
  static{
    mFieldMap.put(UID, JsonLdConstants.ID);
    mFieldMap.put(SUMMARY, "name.@value");
    mFieldMap.put(DESCRIPTION, "description.@value");
    mFieldMap.put(URL, "url");
    mFieldMap.put(LOCATION, "location.address.streetAddress".concat(VALUE_SEPARATOR)
                    .concat("location.address.postalCode").concat(VALUE_SEPARATOR)
                    .concat("location.address.addressLocality").concat(VALUE_SEPARATOR)
                    .concat("location.address.addressRegion").concat(VALUE_SEPARATOR)
                    .concat("location.address.addressCountry").concat(VALUE_SEPARATOR));

    mFieldMap.put(GEO, "location.geo.lat".concat(VALUE_SEPARATOR).concat("location.geo.lon"));
  }

  private final Locale mPreferredLocale;

  public CalendarExporter(Locale aPreferredLocale){
    mPreferredLocale = aPreferredLocale;
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
    return result.toString();
  }

  private String exportResourceWithoutHeader(Resource aResource){
    StringBuilder result = new StringBuilder(EVENT_BEGIN);
    for (Map.Entry<String, String> mapping : mFieldMap.entrySet()){
      boolean hasAppendedSomething = false;
      String[] mappingValues = mapping.getValue().split(VALUE_SEPARATOR);
      for (int i=0; i<mappingValues.length; i++){
        final Object o = aResource.getNestedFieldValue(mappingValues[i], mPreferredLocale);
        if (o != null) {
          String value = o.toString();
          if (i == 0){
            result.append(mapping.getKey());
          } //
          else{
            result.append(VALUE_SEPARATOR);
          }
          result.append(value);
          hasAppendedSomething = true;
        }
      }
      if (hasAppendedSomething){
        result.append("\n");
      }
    }
    result.append(getExportedOrganizer(aResource));
    result.append(getStartDate(aResource));
    result.append(getEndDate(aResource));
    result.append(getTimeStamp());
    result.append(CAL_CLASS);
    result.append(EVENT_END);
    return result.toString();
  }

  private String getExportedOrganizer(Resource aResource){
    StringBuilder result = new StringBuilder();
    List<Resource> organizers = aResource.getAsList("organizer");
    if (organizers != null && !organizers.isEmpty()){
      result.append(ORGANIZER);
      for (Resource organizer : organizers) {
        String name = organizer.getNestedFieldValue("name.@value", mPreferredLocale);
        boolean hasName = false;
        if (name != null) {
          result.append(COMMON_NAME).append(name);
          hasName = true;
        }
        String email = organizer.getAsString("email");
        if (email == null) {
          email = aResource.getAsString("email");
        }
        if (email != null) {
          if (hasName) {
            result.append(KEY_SEPARATOR);
          }
          result.append(MAILTO).append(email);
        }
      }
      result.append("\n");
    }
    return result.toString();
  }

  private String getStartDate(Resource aResource) {
    String originalStartDate = aResource.getAsString("startDate");
    if (originalStartDate == null || Strings.isEmpty(originalStartDate)){
      return null;
    }
    StringBuffer result = new StringBuffer(DATE_START);
    result.append(formatDate(originalStartDate)).append("\n");
    return result.toString();
  }

  private String getEndDate(Resource aResource) {
    String originalEndDate = aResource.getAsString("endDate");
    if (originalEndDate == null || Strings.isEmpty(originalEndDate)){
      return null;
    }
    StringBuffer result = new StringBuffer(DATE_END);
    result.append(formatDate(originalEndDate)).append("\n");
    return result.toString();
  }

  private String formatDate(String aDate){
    Matcher matcher = mDatePattern.matcher(aDate);
    if (matcher.find()){
      return matcher.group(1).concat(matcher.group(2)).concat(matcher.group(3));
    }
    return aDate;
  }

  private String getTimeStamp() {
    return DATE_STAMP.concat(Instant.now().toString().replaceAll("[-:\\.]", "").substring(0, 15)).concat("Z\n");
  }

}
