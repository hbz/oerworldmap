package models;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import helpers.FilesConfig;
import helpers.JsonLdConstants;
import play.Logger;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * @author pvb
 */

public class Action extends ModelCommon implements Comparable<Action> {

  public static final String TYPE = "Action";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static JsonNode mSchemaNode = null;

  // identified ("primary") data types that get an ID
  private static final List<String> mIdentifiedTypes =
    new ArrayList<>(Arrays.asList("LikeAction", "LighthouseAction"));

  static {
    try {
      mSchemaNode = OBJECT_MAPPER.readTree(Paths.get(FilesConfig.getActionSchema()).toFile());
    } catch (IOException e) {
      Logger.error("Could not read schema", e);
    }
  }


  /**
   * Constructor which sets up a random UUID.
   *
   * @param type The type of the action.
   */
  public Action(final String type) {
    this(type, null);
  }

  /**
   * Constructor.
   *
   * @param aType The type of the action.
   * @param aId   The id of the action.
   */
  public Action(final String aType, final String aId) {
    if (null != aType) {
      final Object put = this.put(JsonLdConstants.TYPE, aType);
    }
    if (null != aId) {
      this.put(JsonLdConstants.ID, aId);
    } else if (mIdentifiedTypes.contains(aType)) {
      this.put(JsonLdConstants.ID, generateId());
    }
  }

  public Action(JsonNode aJson) {
    new TypeReference<HashMap<String, Object>>() {
    };
  }

  public Action(Map<String, Object> aProperties) {
    this((String) aProperties.get(JsonLdConstants.TYPE),
      (String) aProperties.get(JsonLdConstants.ID));
    fillFromMap(aProperties, mIdentifiedTypes);
  }


  @Override
  public int compareTo(Action o) {
    return 0;
  }

  @Override
  public String getId() {
    return null;
  }

  @Override
  protected JsonNode getSchemaNode(){
    return mSchemaNode;
  }

  public static List<String> getIdentifiedTypes(){
    return mIdentifiedTypes;
  }
}
