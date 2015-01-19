package models;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Resource {
  
  private static final String JSON_LD_ID_KEY = "@id";
  
  private Map<String, String> mProperties = new HashMap<String, String>();
  public static final Config CONFIG = ConfigFactory.parseFile(new File("conf/application.conf"))
      .resolve();
  /**
   * Add a new key value property pair.
   * @param aKey
   * @param aValue
   * @return an old value associated with the given key
   *         or null if there has not been such old value
   */
  public String addProperty(String aKey, String aValue){
    return mProperties.putIfAbsent(aKey, aValue);
  }
  
  /**
   * Add a map with key value pairs of properties.
   * If some if the keys have existed in the properties
   * before, their associated values will be overwritten.
   * @param aPairs
   */
  public void addProperties(Map<String, String> aPairs){
    mProperties.putAll(aPairs);
  }
  
  /**
   * Generate a UUID and add it to the properties map.
   */
  public void generateUuid(){
    mProperties.put(JSON_LD_ID_KEY, UUID.randomUUID().toString());
  }
  
  /**
   * Get the UUID.
   * @return the UUID or null if no such UUID exists.
   */
  public String getUuid(){
    return mProperties.get(JSON_LD_ID_KEY);
  }
  
  /**
   * Delete all properties containing a null or empty String value.
   * @return all keys with null or empty values as an ArrayList
   *         or null if there have been no such null or empty values.
   */
  public List<String> deleteNullProperties(){
    
    List<String> keysWithNullValues = new ArrayList<String>();
    
    Iterator<Entry<String, String>> it = mProperties.entrySet().iterator();
    while (it.hasNext()) {
        Map.Entry<String, String> pair = (Map.Entry<String, String>)it.next();
        if (StringUtils.isEmpty(pair.getValue())){
          keysWithNullValues.add(pair.getKey());
        }
        mProperties.remove(pair.getKey());
        it.remove();
    }
    return keysWithNullValues.isEmpty() ? null : keysWithNullValues;
  }
  
  @Override
  public String toString() {
    return new JSONObject(mProperties).toString(); 
  }
  
  @Override
  public boolean equals(Object aOther){
    if (! (aOther instanceof Resource)){
      return false;
    }
    Resource other = (Resource) aOther;  
    if (other.mProperties.size() != mProperties.size()){
      return false;
    }
    Iterator<Entry<String, String>> thisIt = mProperties.entrySet().iterator();
    while (thisIt.hasNext()) {
        Map.Entry<String, String> pair = (Map.Entry<String, String>)thisIt.next();
        if (!pair.getValue().equals(other.mProperties.get(pair.getKey()))){
          return false;
        }
        thisIt.remove();
    }
    return true;
  }
}
