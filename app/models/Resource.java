package models;

import helpers.FilesConfig;
import helpers.JsonLdConstants;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
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

public class Resource implements Map<String, Object> {

  /**
   * Holds the properties of the resource.
   */
  private LinkedHashMap<String, Object> mProperties = new LinkedHashMap<String, Object>();

  public Resource() {
    this(null, null);
  }

  /**
   * Constructor which sets up a random UUID.
   * @param type The type of the resource.
   */
  public Resource(String type) {
    this(type, null);
  }

  /**
   * Constructor.
   * @param type The type of the resource.
   * @param id The id of the resource.
   */
  public Resource(String type, String id) {
    if (null != type) {
      mProperties.put(JsonLdConstants.TYPE, type);
    }
    if (null != id) {
      mProperties.put(JsonLdConstants.ID, id);
    }
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

    checkTypeExistence(aProperties);

    Resource resource = new Resource((String) aProperties.get(JsonLdConstants.TYPE),
        (String) aProperties.get(JsonLdConstants.ID));

    for (Map.Entry<String, Object> entry : aProperties.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (value instanceof Map) {
        resource.put(key, Resource.fromMap((Map<String, Object>) value));
      } else if (value instanceof List) {
        List<Object> vals = new ArrayList<>();
        for (Object v : (List) value ) {
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
          "/definitions/".concat(mProperties.get(JsonLdConstants.TYPE).toString()));
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
    return new ObjectMapper().convertValue(mProperties, JsonNode.class);
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
  public int size() {
    return mProperties.size();
  }

  @Override
  public boolean isEmpty() {
    return mProperties.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return mProperties.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return mProperties.containsValue(value);
  }

  @Override
  public Object get(Object key) {
    return mProperties.get(key);
  }

  @Override
  public Object put(String key, Object value) {
    return mProperties.put(key, value);
  }

  @Override
  public Object remove(Object key) {
    return mProperties.remove(key);
  }

  @Override
  public void putAll(Map<? extends String, ? extends Object> m) {
    mProperties.putAll(m);
  }

  @Override
  public void clear() {
    mProperties.clear();
  }

  @Override
  public Set<String> keySet() {
    return mProperties.keySet();
  }

  @Override
  public Collection<Object> values() {
    return mProperties.values();
  }

  @Override
  public Set<Map.Entry<String, Object>> entrySet() {
    return (Set<Map.Entry<String, Object>>) mProperties.entrySet();
  }

  @Override
  public boolean equals(final Object aOther) {
    if (!(aOther instanceof Resource)) {
      return false;
    }
    final Resource other = (Resource) aOther;
    if (other.mProperties.size() != mProperties.size()) {
      return false;
    }
    final Iterator<Map.Entry<String, Object>> thisIt = mProperties.entrySet().iterator();
    while (thisIt.hasNext()) {
      final Map.Entry<String, Object> pair = thisIt.next();
      if (!pair.getValue().equals(other.mProperties.get(pair.getKey()))) {
        return false;
      }
    }
    return true;
  }

  private static boolean hasId(Map<String, Object> aProperties) {
    String id = (String) aProperties.get(JsonLdConstants.ID);
    return id != null && !StringUtils.isEmpty(id.toString());
  }

  private static void checkTypeExistence(Map<String, Object> aProperties) {
    Object type = aProperties.get(JsonLdConstants.TYPE);
    if (type == null) {
      Logger.warn(JsonLdConstants.TYPE + " is null for " + aProperties.toString());
    } else if (!(type instanceof String) || StringUtils.isEmpty(type.toString())) {
      String message = "Unspecified " + JsonLdConstants.TYPE + " : " + aProperties.hashCode();
      Logger.error(message);
      try {
        throw new java.lang.TypeNotPresentException(message, new Exception());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

}
