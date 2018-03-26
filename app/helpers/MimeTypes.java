package helpers;

import play.mvc.Http.Request;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by fo on 29.08.17.
 */
public class MimeTypes {

  public static final String MIME_APPLICATION_JSON = "application/json";
  public static final String MIME_APPLICATION_GEOJSON = "application/geo+json";
  public static final String MIME_APPLICATION_SCHEMAJSON = "application/schema+json";
  public static final String MIME_TEXT_CSV = "text/csv";
  public static final String MIME_TEXT_CALENDAR = "text/calendar";

  private static Map<String, String> mimeTypeMapping = new HashMap<>();

  static {
    mimeTypeMapping.put("json", MIME_APPLICATION_JSON);
    mimeTypeMapping.put("geojson", MIME_APPLICATION_GEOJSON);
    //mimeTypeMapping.put("schema", MIME_APPLICATION_SCHEMAJSON);
    mimeTypeMapping.put("csv", MIME_TEXT_CSV);
    mimeTypeMapping.put("ics", MIME_TEXT_CALENDAR);
  }

  public static Map<String, String> all() {
    return new HashMap<>(mimeTypeMapping);
  }

  public static String fromExtension(String ext) {
    return mimeTypeMapping.get(ext);
  }

  public static String fromRequest(Request request) {

    String mimeType;

    if (request.accepts(MIME_TEXT_CSV)) {
      mimeType = MIME_TEXT_CSV;
    } else if (request.accepts(MIME_TEXT_CALENDAR)) {
      mimeType = MIME_TEXT_CALENDAR;
    } else if (request.accepts(MIME_APPLICATION_GEOJSON)) {
      mimeType = MIME_APPLICATION_GEOJSON;
    } else if (request.accepts(MIME_APPLICATION_SCHEMAJSON)) {
      mimeType = MIME_APPLICATION_SCHEMAJSON;
    } else {
      mimeType = MIME_APPLICATION_JSON;
    }

    return mimeType;

  }

}
