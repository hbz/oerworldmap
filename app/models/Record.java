package models;

import helpers.JsonLdConstants;
import helpers.UniversalFunctions;

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
