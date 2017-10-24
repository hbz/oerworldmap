package models;

import helpers.JsonLdConstants;

import java.util.*;

/**
 * @author pvb
 */
public abstract class ModelCommon extends HashMap<String, Object> {

  public Map<?, ?> getAsMap(final String aKey) {
    Object result = get(aKey);
    return (null == result || !(result instanceof Map<?, ?>)) ? null : (ModelCommon) result;
  }


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

  public boolean hasId() {
    return containsKey(JsonLdConstants.ID);
  }

  public String getId() {
    return getAsString(JsonLdConstants.ID);
  }

  public String getType() {
    return getAsString(JsonLdConstants.TYPE);
  }

  public List<ModelCommon> getAsList(final Object aKey) {
    List<ModelCommon> list = new ArrayList<>();
    Object result = get(aKey);
    if (result instanceof Resource) {
      list.add((Resource) result);
    } else if (result instanceof List<?>) {
      for (Object value : (List<?>) result) {
        if (value instanceof Resource) {
          list.add((Resource) value);
        }
      }
    }
    return list;
  }


  public String getNestedFieldValue(final String aNestedKey, final Locale aPreferredLocale){
    final String[] split = aNestedKey.split("\\.", 2);
    if (split.length == 0){
      return null;
    }
    if (split.length == 1){
      Object o = get(split[0]);
      if (o != null) {
        return o.toString();
      }
      return null;
    }
    // split.length == 2
    final Object o = get(split[0]);
    if (o instanceof ArrayList<?>){
      String next = getNestedValueOfList(split[1], (ArrayList<?>) o, aPreferredLocale);
      if (next != null) return next;
    } //
    else if (o instanceof Resource){
      Resource resource = (Resource) o;
      if (resource.size() == 0){
        return null;
      }
      return resource.getNestedFieldValue(split[1], aPreferredLocale);
    }
    return null;
  }

  private String getNestedValueOfList(final String aKey, final ArrayList<?> aList, final Locale aPreferredLocale) {
    Object next;
    final Locale fallbackLocale = Locale.ENGLISH;
    String fallback1 = null;
    String fallback2 = null;
    String fallback3 = null;
    for (Iterator it = aList.iterator(); it.hasNext(); ){
      next = it.next();
      if (next instanceof Resource){
        Resource resource = (Resource) next;
        Object language = resource.get("@language");
        if (language.equals(aPreferredLocale.getLanguage())){
          return resource.getNestedFieldValue(aKey, aPreferredLocale);
        }
        if (language == null){
          fallback1 = resource.getNestedFieldValue(aKey, aPreferredLocale);
        }
        else if (language.equals(fallbackLocale.getLanguage())){
          fallback2 = resource.getNestedFieldValue(aKey, fallbackLocale);
        }
        else {
          fallback3 = resource.getNestedFieldValue(aKey, Locale.forLanguageTag(language.toString()));
        }
      }
    }
    return (fallback1 != null) ? fallback1 : (fallback2 != null) ? fallback2 : fallback3;
  }

}
