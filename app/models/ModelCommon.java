package models;

import java.util.HashMap;

/**
 * @author pvb
 */
public abstract class ModelCommon extends HashMap<String, Object> {

  public abstract String getId();

  public String getAsString(final Object aKey) {
    Object result = get(aKey);
    return (result == null) ? null : result.toString();
  }

  public ModelCommon getAsItem(final Object aKey) {
    Object result = get(aKey);
    if (null == result){
      return null;
    }
    if (result instanceof Resource){
      return (Resource) result;
    }
    if (result instanceof Action){
      return (Action) result;
    }
    return null;
  }

}
