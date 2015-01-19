package models;

import helpers.JsonLdConstants;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Resource {

  /**
   * These properties cannot be set after construction.
   */
  private String[] mReadOnlyPropertyList = {JsonLdConstants.TYPE, JsonLdConstants.ID};

  /**
   * Holds the properties of the resource.
   */
  private LinkedHashMap<String, Object> mProperties = new LinkedHashMap<String, Object>();

  /**
   * Constructor.
   *
   * @param   type  The type of the resource.
   */
  public Resource(String type) {
    mProperties.put(JsonLdConstants.TYPE, type);
    String uuid = UUID.randomUUID().toString();
    mProperties.put(JsonLdConstants.ID, uuid);
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
   * Set the value of a property of the resource.
   *
   * @param   property  The property to set.
   * @param   value     The value of the property.
   */
  public void set(String property, Object value) throws UnsupportedOperationException {
    if (Arrays.asList(mReadOnlyPropertyList).contains(property)) {
      throw new UnsupportedOperationException();
    }
    mProperties.put(property, value);
  }

  /**
   * Get the value of a property of the resource.
   *
   * @param   property  The property to get.
   * @return  The value of the property.
   */
  public Object get(String property) {
    return mProperties.get(property);
  }

  /**
   * Get a JSON string representation of the resource.
   *
   * @return JSON string
   */
  @Override
  public String toString() {
    try {
      return new ObjectMapper().writeValueAsString(mProperties);
    } catch (IOException e) {
      return "";
    }
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
        thisIt.remove();
    }
    return true;
  }
  
}

