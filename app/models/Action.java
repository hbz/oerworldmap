package models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import helpers.FilesConfig;
import play.Logger;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author pvb
 */

// TODO: un-dummy

public class Action extends ModelCommon implements Comparable<Action> {

  public static final String TYPE = "Action";
  private static final ObjectMapper mObjectMapper = new ObjectMapper();
  private static JsonNode mSchemaNode = null;

  static {
    try {
      mSchemaNode = mObjectMapper.readTree(Paths.get(FilesConfig.getActionSchema()).toFile());
    } catch (IOException e) {
      Logger.error("Could not read schema", e);
    }
  }

  @Override
  public int compareTo(Action o) {
    return 0;
  }

  @Override
  public String getId() {
    return null;
  }
}
