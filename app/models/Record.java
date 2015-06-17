package models;

import helpers.JsonLdConstants;
import helpers.UniversalFunctions;

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
    put("dateCreated", UniversalFunctions.getCurrentTime());
  }

  public Resource getResource() {
    return (Resource) get(RESOURCEKEY);
  }


}
