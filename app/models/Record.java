package models;

import com.fasterxml.jackson.databind.JsonNode;
import helpers.JsonLdConstants;
import helpers.UniversalFunctions;
import services.export.GeoJsonExporter;

public class Record extends Resource {

  /**
   *
   */
  private static final long serialVersionUID = 5181258925743099684L;
  private static GeoJsonExporter mGeoJsonExporter = new GeoJsonExporter();
  public static final String TYPE = "WebPage";
  public static final String RESOURCE_KEY = "about";
  public static final String DATE_CREATED = "dateCreated";
  public static final String DATE_MODIFIED = "dateModified";
  public static final String AUTHOR = "author";
  public static final String CONTRIBUTOR = "contributor";
  public static final String LINK_COUNT = "link_count";
  public static final String FEATURE = "feature";
  public static final String LIKE_COUNT = "like_count";
  public static final String LIGHTHOUSE_COUNT = "lighthouse_count";

  public Record(Resource aResource) {
    super(TYPE, aResource.get(JsonLdConstants.ID) + "." + RESOURCE_KEY);
    put(RESOURCE_KEY, aResource);
    put(DATE_MODIFIED, UniversalFunctions.getCurrentTime());
    JsonNode geoJson = mGeoJsonExporter.exportJson(this);
    if (geoJson != null) {
      put(FEATURE, geoJson);
    }
  }

  public Resource getResource() {
    return getAsResource(RESOURCE_KEY);
  }
}
