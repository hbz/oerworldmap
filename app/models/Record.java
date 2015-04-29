package models;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

public class Record extends Resource {

  public Record(Resource aResource) {
    super("WebPage", aResource.get("@id") + ".about");
    put("about", aResource);
    put("dateModified", getCurrentTime());
  }

  public Resource getResource() {
    return (Resource) get("about");
  }

  private String getCurrentTime() {
    TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
    df.setTimeZone(tz);
    return df.format(new Date());
  }
  
  public static Record fromMap(Map<String, Object> aProperties){
    return (Record) Resource.fromMap(aProperties);
  }
}
