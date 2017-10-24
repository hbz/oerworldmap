package services.export;

import helpers.JsonLdConstants;
import models.Record;
import models.Resource;
import models.ResourceList;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.Strings;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
  private static final String ORGANIZER = "ORGANIZER";
  private static final String COMMON_NAME = ";CN=";      // in line ORGANIZER
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
  private static final String DEFAULT_TIME_START = "T000000";
  private static final String DEFAULT_TIME_END = "T235959";

  private static final String SIMPLE_DATE_REGEX = "^([\\d]{4})-([\\d]{2})-([\\d]{2})$";
  private static final Pattern mSimpleDatePattern = Pattern.compile(SIMPLE_DATE_REGEX);

  private static final Map<String, String> mFieldMap = new HashMap<>();

  static{
    mFieldMap.put(UID, JsonLdConstants.ID);
    mFieldMap.put(SUMMARY, "name.@value");
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
    if (!aResource.getAsItem(Record.RESOURCE_KEY).getType().equals("Event")) {
      return null;
    }
    return HEADER.concat(exportResourceWithoutHeader(aResource.getAsItem(Record.RESOURCE_KEY))).concat(FOOTER);
  }

  @Override
  public String export(ResourceList aResourceList) {
    StringBuilder result = new StringBuilder(HEADER);
    aResourceList.getItems().stream().filter(resource -> resource.getAsResource(Record.RESOURCE_KEY).getType()
      .equals("Event")).forEach(resource ->
        result.append(exportResourceWithoutHeader(resource.getAsResource(Record.RESOURCE_KEY)))
    );
    result.append(FOOTER);
    return result.toString();
  }

  private String exportResourceWithoutHeader(final Resource aResource){
    final String startDate = parseStartDate(aResource);
    final String dateStamp = getTimeStamp();
    // Check required fields according to https://tools.ietf.org/html/rfc5545#section-3.6.1
    if (StringUtils.isEmpty(startDate) || StringUtils.isEmpty(dateStamp)){
      // Missing UID is not checked here because it can never be null for an existing resource.
      return "";
    }
    final StringBuilder result = new StringBuilder(EVENT_BEGIN);
    for (Map.Entry<String, String> mapping : mFieldMap.entrySet()){
      boolean hasAppendedSomething = false;
      String[] mappingValues = mapping.getValue().split(VALUE_SEPARATOR);
      StringBuilder subResult = new StringBuilder();
      for (String mappingValue : mappingValues) {
        final String value = aResource.getNestedFieldValue(mappingValue, mPreferredLocale);
        if (value != null && !Strings.isEmpty(value)) {
          if (subResult.length() == 0) {
            subResult.append(mapping.getKey());
          } //
          else {
            if (subResult.length() > mapping.getKey().length()) {
              subResult.append(VALUE_SEPARATOR);
            }
          }
          subResult.append(value);
          hasAppendedSomething = true;
        }
      }
      result.append(subResult);
      if (hasAppendedSomething){
        result.append("\n");
      }
    }
    result.append(getDescription(aResource));
    result.append(getExportedOrganizer(aResource));
    result.append(startDate);
    result.append(parseEndDate(aResource));
    result.append(dateStamp);
    result.append(CAL_CLASS);
    completeFields(result, aResource);
    result.append(EVENT_END);
    return result.toString();
  }

  private void completeFields(final StringBuilder aStringBuilder, final Resource aResource){
    if (aResource.getNestedFieldValue("name.@value", mPreferredLocale) == null){
      // no summary could be added --> append empty summary to comply to ical specification
      aStringBuilder.append("SUMMARY:\n");
    }
  }

  private String getExportedOrganizer(Resource aResource){
    StringBuilder result = new StringBuilder();
    List<Resource> organizers = aResource.getAsList("organizer");
    result.append(ORGANIZER);
    boolean hasOrganizer = false;
    if (organizers != null && !organizers.isEmpty()){
      for (Resource organizer : organizers) {
        String name = organizer.getNestedFieldValue("name.@value", mPreferredLocale);
        String email = organizer.getAsString("email");
        boolean hasName = false;
        if (name != null) {
          if (email == null){
            result.append(":").append(name);
          }
          else {
            result.append(COMMON_NAME).append("\"").append(name).append("\"");
          }
          hasOrganizer = true;
          hasName = true;
        }
        if (email == null) {
          email = aResource.getAsString("email");
        }
        if (email != null) {
          if (hasName) {
            result.append(KEY_SEPARATOR);
          }
          result.append(MAILTO).append(email);
        }
        if (hasName){
          break;
        }
      }
      result.append("\n");
    }
    if (!hasOrganizer){
      result.append(":\n");
    }
    return result.toString();
  }

  private String parseStartDate(final Resource aResource) {
    final String originalStartDate = aResource.getAsString("startDate");
    StringBuilder result = new StringBuilder();
    if (originalStartDate == null || Strings.isEmpty(originalStartDate)){
      return "";
    }
    if (!appendDate(originalStartDate, result, DATE_START, DEFAULT_TIME_START)){
      return "";
    }
    return result.append("\n").toString();
  }

  private static String parseEndDate(final Resource aResource) {
    final String originalEndDate = aResource.getAsString("endDate");
    StringBuilder result = new StringBuilder();
    if (originalEndDate == null || Strings.isEmpty(originalEndDate)){
      return "";
    }
    if (!appendDate(originalEndDate, result, DATE_END, DEFAULT_TIME_END)){
      return "";
    }
    return result.append("\n").toString();
  }

  private static boolean appendDate(final String aOriginalDate, final StringBuilder aResult,
                                    final String aDateLineHeader, final String aDefaultTimeString) {
    Matcher matcher = mSimpleDatePattern.matcher(aOriginalDate);
    if (matcher.find()) {
      aResult.append(aDateLineHeader)
        .append(formatSimpleDate(matcher, aDefaultTimeString));
    }
    else {
      final DateTime dateTime = parseISO8601toUTC(aOriginalDate);
      if (dateTime == null) {
        return false;
      }
      aResult.append(dateTimeToIcalDate(aDateLineHeader, dateTime));
    }
    return true;
  }

  private static String formatSimpleDate(final Matcher aMatcher, final String aTime){
    return aMatcher.group(1).concat(aMatcher.group(2)).concat(aMatcher.group(3)).concat(aTime);
  }

  private static StringBuilder dateTimeToIcalDate(final String aField, final DateTime aDateTime) {
    final StringBuilder result = new StringBuilder(aField);
    result.append(String.format("%04d", aDateTime.getYear()))
      .append(String.format("%02d", aDateTime.getMonthOfYear()))
      .append(String.format("%02d", aDateTime.getDayOfMonth()))
      .append("T")
      .append(String.format("%02d", aDateTime.getHourOfDay()))
      .append(String.format("%02d", aDateTime.getMinuteOfHour()))
      .append(String.format("%02d", aDateTime.getSecondOfMinute()));
    return result;
  }

  /**
   * found on: http://www.javased.com/?api=org.joda.time.format.ISODateTimeFormat
   *
   * From project Carolina-Digital-Repository, under directory /metadata/src/main/java/edu/unc/lib/dl/util/
   *
   * Parse a date in any ISO 8601 format. Default TZ is based on Locale.
   * @param isoDate ISO8601 date/time string with or without TZ offset
   * @return a Joda DateTime object in UTC (call toString() to print)
   */
  private static DateTime parseISO8601toUTC(String isoDate){
    DateTime result;
    DateTimeFormatter fmt= ISODateTimeFormat.dateTimeParser().withOffsetParsed();
    DateTime isoDT=fmt.parseDateTime(isoDate);
    if (isoDT.year().get() > 9999) {
      try {
        fmt= DateTimeFormat.forPattern("yyyyMMdd");
        fmt=fmt.withZone(DateTimeZone.getDefault());
        isoDT=fmt.parseDateTime(isoDate);
      }
      catch (IllegalArgumentException e) {
        try {
          fmt=DateTimeFormat.forPattern("yyyyMM");
          fmt=fmt.withZone(DateTimeZone.getDefault());
          isoDT=fmt.parseDateTime(isoDate);
        }
        catch (IllegalArgumentException e1) {
          try {
            fmt=DateTimeFormat.forPattern("yyyy");
            fmt=fmt.withZone(DateTimeZone.getDefault());
            isoDT=fmt.parseDateTime(isoDate);
          }
          catch (IllegalArgumentException ignored) {
          }
        }
      }
    }
    result=isoDT.withZoneRetainFields(DateTimeZone.getDefault());
    return result;
  }

  private String getTimeStamp() {
    return DATE_STAMP.concat(Instant.now().toString().replaceAll("[-:\\.]", "").substring(0, 15)).concat("Z\n");
  }

  private String getDescription(Resource aResource){
    String description = aResource.getNestedFieldValue("description.@value", mPreferredLocale);
    if (description == null || Strings.isEmpty(description)){
      return "";
    }
    return DESCRIPTION.concat(description.replaceAll("\r\n|\n|\r", "\\\\r\\\\n")).concat("\n");
  }

}
