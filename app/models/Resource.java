package models;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import helpers.JsonLdConstants;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Resource implements Map<String, Object> {

  /**
   * Holds the properties of the resource.
   */
  private LinkedHashMap<String, Object> mProperties = new LinkedHashMap<String, Object>();

  /**
   * Constructor which sets up a random UUID.
   *
   * @param   type  The type of the resource.
   */
  public Resource(String type) {
    this(type, UUID.randomUUID().toString());
  }

  /**
   * Constructor.
   *
   * @param   type  The type of the resource.
   * @param   id    The id of the resource.
   */
  public Resource(String type, String id) {
    mProperties.put(JsonLdConstants.TYPE, type);
    mProperties.put(JsonLdConstants.ID, id);
  }

  /**
   * Convert a Map of String/Object to a Resource, assuming that all
   * Object values of the map are properly represented by the toString()
   * method of their class.
   *
   * @param   aProperties  The map to create the resource from
   * @return  a Resource containing all given properties
   */
  public static Resource fromMap(Map<String, Object> aProperties) {

    checkTypeExistence(aProperties);
    Resource resource;
    if (hasId(aProperties)) {
      resource = new Resource((String) aProperties.get(JsonLdConstants.TYPE),
                              (String) aProperties.get(JsonLdConstants.ID));
    } else {
      resource = new Resource((String) aProperties.get(JsonLdConstants.TYPE));
    }
    Iterator<Entry<String, Object>> it = aProperties.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, Object> pair = (Map.Entry<String, Object>) it.next();
      String key = pair.getKey();
      Object value = pair.getValue();
      if (value instanceof Map<?, ?>) {
        resource.put(key, Resource.fromMap((Map<String, Object>) value));
      } else {
        resource.put(key, value);
      }
    }
    return resource;
  }

  public static Resource fromJson(JsonNode aJson) {
    Map<String, Object> resourceMap = new ObjectMapper().convertValue(aJson,
        new TypeReference<HashMap<String, Object>>(){});
    return fromMap(resourceMap);
  }

  public ProcessingReport validate() {
    JsonSchema schema;
    ProcessingReport report;
    try {
      schema = JsonSchemaFactory.byDefault().getJsonSchema(
          new ObjectMapper().readTree(Paths.get("public/json/schema.json").toFile()),
              "/".concat(mProperties.get(JsonLdConstants.TYPE).toString())
      );
      report = schema.validate(toJson());
    } catch (ProcessingException | IOException e) {
      report = new ListProcessingReport();
      e.printStackTrace();
    }
    return report;
  }

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
    return toJson().toString();
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
  public Set<Entry<String, Object>> entrySet() {
    return (Set<Entry<String, Object>>) mProperties.entrySet();
  }

  @Override
  public boolean equals(final Object aOther){
    if (! (aOther instanceof Resource)){
      return false;
    }
    final Resource other = (Resource) aOther;
    if (other.mProperties.size() != mProperties.size()){
      return false;
    }
    final Iterator<Entry<String, Object>> thisIt = mProperties.entrySet().iterator();
    while (thisIt.hasNext()) {
        final Map.Entry<String, Object> pair = thisIt.next();
        if (!pair.getValue().equals(other.mProperties.get(pair.getKey()))){
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
    if (!(type instanceof String) || StringUtils.isEmpty((String) type)) {
      String message = "Unspecified " + JsonLdConstants.TYPE + " : " + aProperties.hashCode();
      System.err.println(message);
      try {
        throw new java.lang.TypeNotPresentException(message, new Exception());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

}

