package models;

import com.fasterxml.jackson.databind.JsonNode;
import helpers.JsonLdConstants;

import java.io.IOException;
import java.util.*;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

public class Resource implements Map {

  /**
   * Holds the properties of the resource.
   */
  private LinkedHashMap<Object, Object> mProperties = new LinkedHashMap<Object, Object>();

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
        resource.put(pair.getKey(), pair.getValue().toString());
      it.remove();
    }
    return resource;
  }
  
  /**
   * Get a JSON string representation of the resource.
   *
   * @return JSON string
   */
  @Override
  public String toString() {
    return new ObjectMapper().convertValue(mProperties, JsonNode.class).toString();
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
  public Object put(Object key, Object value) {
    return mProperties.put(key, value);
  }

  @Override
  public Object remove(Object key) {
    return mProperties.remove(key);
  }

  @Override
  public void putAll(Map m) {
    mProperties.putAll(m);
  }

  @Override
  public void clear() {
    mProperties.clear();
  }

  @Override
  public Set keySet() {
    return mProperties.keySet();
  }

  @Override
  public Collection values() {
    return mProperties.values();
  }

  @Override
  public Set<Entry> entrySet() {
    return (Set) mProperties.entrySet();
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
    final Iterator<Entry<Object, Object>> thisIt = mProperties.entrySet().iterator();
    while (thisIt.hasNext()) {
        final Map.Entry<Object, Object> pair = thisIt.next();
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

