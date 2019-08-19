package services.export;

import models.Record;
import models.Resource;
import models.ResourceList;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Clazz;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.DtStamp;
import net.fortuna.ical4j.model.property.Geo;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Method;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Url;
import net.fortuna.ical4j.model.property.Version;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Created by fo and pvb
 */
public class CalendarExporter implements Exporter {

  private final Locale preferredLocale;
  private final String dateStamp;

  private DateTimeFormatter formatter = new DateTimeFormatterBuilder()
    .appendPattern("yyyy[-MM[-dd['T'HH:mm[:ss]]]]")
    .toFormatter();

  public CalendarExporter(Locale preferredLocale) {
    this.preferredLocale = preferredLocale;
    this.dateStamp = null;
  }

  public CalendarExporter(Locale preferredLocale, String dateStamp) {
    this.preferredLocale = preferredLocale;
    this.dateStamp = dateStamp;
  }

  @Override
  public String export(Resource record) {
    Resource resource = record.getAsResource(Record.RESOURCE_KEY);
    if (!resource.getType().equals("Event")) {
      return null;
    }
    VEvent event = resourceToEvent(resource);
    Calendar calendar = initCalendar();
    if (event != null) {
      calendar.getComponents().add(event);
    }
    return calendar.toString();
  }

  @Override
  public String export(ResourceList aResourceList) {
    Calendar calendar = initCalendar();
    for (Resource record: aResourceList.getItems()) {
      Resource resource = record.getAsResource(Record.RESOURCE_KEY);
      if (resource.getType().equals("Event")) {
        VEvent event = resourceToEvent(resource);
        if (event != null) {
          calendar.getComponents().add(event);
        }
      }
    }
    return calendar.toString();
  }

  private Calendar initCalendar() {
    Calendar calendar = new Calendar();
    calendar.getProperties().add(new ProdId("https://oerworldmap.org/"));
    calendar.getProperties().add(Version.VERSION_2_0);
    calendar.getProperties().add(CalScale.GREGORIAN);
    calendar.getProperties().add(Method.PUBLISH);
    return calendar;
  }

  private VEvent resourceToEvent(Resource resource) {
    Map<String, String> pointerDict = resource.toPointerDict();
    String startDate = pointerDict.get("/startDate");
    if (startDate == null) {
      return null;
    }
    String endDate = pointerDict.get("/endDate");
    String organizer = pointerDict.get("/organizer/0/name/en");
    String email = pointerDict.get("/organizer/0/email");
    String location = String.join(";", Arrays.asList(
      pointerDict.getOrDefault("/location/0/address/streetAddress", ""),
      pointerDict.getOrDefault("/location/0/address/postalCode", ""),
      pointerDict.getOrDefault("/location/0/address/addressLocality", ""),
      pointerDict.getOrDefault("/location/0/address/addressRegion", ""),
      pointerDict.getOrDefault("/location/0/address/addressCountry", "")
    ));
    String lat = pointerDict.get("/location/0/geo/lat");
    String lon = pointerDict.get("/location/0/geo/lon");
    String url = pointerDict.get("/url");
    String name = pointerDict.get("/name/en");
    String description = pointerDict.get("/description/en");

    VEvent event;
    if (endDate != null) {
      event = new VEvent(parseDate(startDate), parseDate(endDate), name);
    } else {
      event = new VEvent(parseDate(startDate), name);
    }

    PropertyList<Property> propertyList = event.getProperties();
    propertyList.add(new Uid(resource.getId()));
    if (this.dateStamp != null) {
      try {
        propertyList.add(new DtStamp(this.dateStamp));
      } catch (ParseException e) {
        // ignore
      }
    }
    if (organizer != null) {
      Organizer organizerProp;
      if (email != null) {
        try {
          organizerProp = new Organizer("mailto:".concat(email));
        } catch (URISyntaxException e) {
          organizerProp = new Organizer();
        }
      } else {
        organizerProp = new Organizer();
      }
      organizerProp.getParameters().add(new Cn(organizer));
      propertyList.add(organizerProp);
    }
    propertyList.add(new Location(location));
    propertyList.add(Clazz.PUBLIC);
    if (lat != null && lon != null) {
      propertyList.add(new Geo(new BigDecimal(lat), new BigDecimal(lon)));
    }
    if (url != null) {
      try {
        propertyList.add(new Url(new URI(url)));
      } catch (URISyntaxException e) {
        // ignore
      }
    }
    if (description != null) {
      propertyList.add(new Description(description));
    }
    return event;
  }

  private Date parseDate(String date) {
    TemporalAccessor temporalAccessor = formatter.parse(date);
    java.util.Calendar calendar = java.util.Calendar.getInstance();
    if (temporalAccessor.isSupported(ChronoField.YEAR)) {
      calendar.set(java.util.Calendar.YEAR, temporalAccessor.get(ChronoField.YEAR));
    }
    if (temporalAccessor.isSupported(ChronoField.MONTH_OF_YEAR)) {
      calendar.set(java.util.Calendar.MONTH, temporalAccessor.get(ChronoField.MONTH_OF_YEAR) - 1);
    }
    if (temporalAccessor.isSupported(ChronoField.DAY_OF_MONTH)) {
      calendar.set(java.util.Calendar.DAY_OF_MONTH, temporalAccessor.get(ChronoField.DAY_OF_MONTH));
    }
    if (temporalAccessor.isSupported(ChronoField.HOUR_OF_DAY)) {
      calendar.set(java.util.Calendar.HOUR_OF_DAY, temporalAccessor.get(ChronoField.HOUR_OF_DAY));
    }
    if (temporalAccessor.isSupported(ChronoField.MINUTE_OF_HOUR)) {
      calendar.set(java.util.Calendar.MINUTE, temporalAccessor.get(ChronoField.MINUTE_OF_HOUR));
    }
    if (temporalAccessor.isSupported(ChronoField.SECOND_OF_MINUTE)) {
      calendar.set(java.util.Calendar.SECOND, temporalAccessor.get(ChronoField.SECOND_OF_MINUTE));
    }
    if (temporalAccessor.isSupported(ChronoField.YEAR) && temporalAccessor.isSupported(ChronoField.HOUR_OF_DAY)) {
      return new DateTime(calendar.getTime());
    }
    return new Date(calendar.getTime());
  }

}
