package models;

import helpers.JsonLdConstants;
import helpers.UniversalFunctions;

public class Record extends Resource {

  /**
   * 
   */
  private static final long serialVersionUID = 5181258925743099684L;
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
