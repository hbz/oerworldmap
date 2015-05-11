package models;

import helpers.JsonLdConstants;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

public class Record extends Resource {

  public static final String RESOURCEKEY = "about";

  public Record(Resource aResource) {
    super("WebPage", aResource.get(JsonLdConstants.ID) + "." + RESOURCEKEY);
    put(RESOURCEKEY, aResource);
    put("dateModified", getCurrentTime());
  }

  public Resource getResource() {
    return (Resource) get(RESOURCEKEY);
  }

  private String getCurrentTime() {
    TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
    df.setTimeZone(tz);
    return df.format(new Date());
  }

}
