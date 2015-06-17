package models;

import helpers.FilesConfig;
import helpers.JsonLdConstants;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import play.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

public class Resource extends HashMap<String, Object> {

  // identified ("primary") data types that get an ID
  private static final List<String> mIdentifiedTypes = new ArrayList<String>(Arrays.asList(
      "Organization", "Event", "Person", "Action", "WebPage", "Article", "Service"));

  public static final String REFERENCEKEY = "referencedBy";

  public Resource() {
    this(null, null);
  }

  /**
   * Constructor which sets up a random UUID.
   *
   * @param type The type of the resource.
   */
  public Resource(final String type) {
    this(type, null);
  }

  /**
   * Constructor.
   *
   * @param aType The type of the resource.
   * @param aId The id of the resource.
   */
  public Resource(final String aType, final String aId) {
    if (null != aType) {
      this.put(JsonLdConstants.TYPE, aType);
    }
    if (mIdentifiedTypes.contains(aType)) {
      if (null != aId) {
        this.put(JsonLdConstants.ID, aId);
      } else {
        this.put(JsonLdConstants.ID, generateId());
      }
    }
  }

  private static String generateId() {
    return "urn:uuid:" + UUID.randomUUID().toString();
  }

  /**
   * Convert a Map of String/Object to a Resource, assuming that all Object
   * values of the map are properly represented by the toString() method of
   * their class.
   *
   * @param aProperties The map to create the resource from
   * @return a Resource containing all given properties
   */
  @SuppressWarnings("unchecked")
  public static Resource fromMap(Map<String, Object> aProperties) {

    if (aProperties == null){
      return null;
    }

    String type = (String) aProperties.get(JsonLdConstants.TYPE);
    String id = (String) aProperties.get(JsonLdConstants.ID);
    Resource resource = new Resource(type, id);

    for (Map.Entry<String, Object> entry : aProperties.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (key.equals(JsonLdConstants.ID) && ! mIdentifiedTypes.contains(type)) {
        continue;
      }
      if (value instanceof Map) {
        resource.put(key, Resource.fromMap((Map<String, Object>) value));
      } else if (value instanceof List) {
        List<Object> vals = new ArrayList<>();
        for (Object v : (List) value) {
          if (v instanceof Map) {
            vals.add(Resource.fromMap((Map<String, Object>) v));
          } else {
            vals.add(v);
          }
        }
        resource.put(key, vals);
      } else {
        resource.put(key, value);
      }
    }

    return resource;

  }

  public static Resource fromJson(JsonNode aJson) {
    Map<String, Object> resourceMap = new ObjectMapper().convertValue(aJson,
        new TypeReference<HashMap<String, Object>>() {
        });
    return fromMap(resourceMap);
  }

  public ProcessingReport validate() {
    JsonSchema schema;
    ProcessingReport report;
    try {
      schema = JsonSchemaFactory.byDefault().getJsonSchema(
          new ObjectMapper().readTree(Paths.get(FilesConfig.getSchema()).toFile()),
          "/definitions/".concat(this.get(JsonLdConstants.TYPE).toString()));
      report = schema.validate(toJson());
    } catch (ProcessingException | IOException e) {
      report = new ListProcessingReport();
      e.printStackTrace();
    }
    return report;
  }

  /**
   * Get a JsonNode representation of the resource.
   *
   * @return JSON JsonNode
   */
  public JsonNode toJson() {
    return new ObjectMapper().convertValue(this, JsonNode.class);
  }

  /**
   * Get a JSON string representation of the resource.
   *
   * @return JSON string
   */
  @Override
  public String toString() {
    ObjectMapper mapper = new ObjectMapper();
    String output;
    try {
      output = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(toJson());
    } catch (JsonProcessingException e) {
      output = toJson().toString();
      e.printStackTrace();
    }
    return output;
  }

  @Override
  public boolean containsKey(Object key) {
    String keyString = key.toString();
    if (keyString.startsWith("?")) {
      return keyString.substring(1).equals(this.get(JsonLdConstants.TYPE));
    }
    return super.containsKey(key);
  }

  @Override
  public Object get(Object key) {
    String keyString = key.toString();
    if (keyString.startsWith("?")) {
      return keyString.substring(1).equals(this.get(JsonLdConstants.TYPE));
    }
    return super.get(key);
  }

  @Override
  public boolean equals(final Object aOther) {
    if (!(aOther instanceof Resource)) {
      return false;
    }
    final Resource other = (Resource) aOther;
    if (other.size() != this.size()) {
      return false;
    }
    final Iterator<Map.Entry<String, Object>> thisIt = this.entrySet().iterator();
    while (thisIt.hasNext()) {
      final Map.Entry<String, Object> pair = thisIt.next();
      if (!pair.getValue().equals(other.get(pair.getKey()))) {
        return false;
      }
    }
    return true;
  }

  public boolean hasId() {
    return containsKey(JsonLdConstants.ID);
  }

}
