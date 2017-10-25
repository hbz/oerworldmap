package models;

import com.fasterxml.jackson.databind.JsonNode;
import helpers.JsonLdConstants;
import helpers.UniversalFunctions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Record extends ModelCommon implements Comparable<Record> {

  /**
   *
   */
  private static final long serialVersionUID = 5181258925743099684L;
  public static final String CONTENT_KEY = "about";
  public static final String DATE_CREATED = "dateCreated";
  public static final String DATE_MODIFIED = "dateModified";
  public static final String AUTHOR = "author";
  public static final String CONTRIBUTOR = "contributor";
  public static final String LINK_COUNT = "link_count";
  public static final String LIKE_COUNT = "like_count";

  private static List<String> mIdentifiedTypes = new ArrayList<>(Arrays.asList("WebPage"));

  public Record(final ModelCommon aItem, final String aIndexType) {
    this.put(JsonLdConstants.TYPE, aIndexType);
    if (null != aItem.get(JsonLdConstants.ID)) {
      this.put(JsonLdConstants.ID, aItem.get(JsonLdConstants.ID) + "." + CONTENT_KEY);
    } else if (mIdentifiedTypes.contains(aItem.getType())) {
      this.put(JsonLdConstants.ID, generateId());
    }
    put(CONTENT_KEY, aItem);
    put(DATE_MODIFIED, UniversalFunctions.getCurrentTime());
  }

  public ModelCommon getRecordContent() {
    return (ModelCommon) get(CONTENT_KEY);
  }

  public static List<String> getIdentifiedTypes() {
    return mIdentifiedTypes;
  }

  @Override
  public int compareTo(Record aOther) {
    if (hasId() && aOther.hasId()) {
      return getAsString(JsonLdConstants.ID).compareTo(aOther.getAsString(JsonLdConstants.ID));
    }
    return toString().compareTo(aOther.toString());
  }

  @Override
  public JsonNode getSchemaNode(){
    throw new UnsupportedOperationException("Record is not based on schema.");
  }

}
