package models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import helpers.JsonLdConstants;
import play.Logger;

import java.util.*;

/**
 * @author pvb
 */
public abstract class ModelCommon extends HashMap<String, Object> {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  protected static String generateId() {
    return "urn:uuid:" + UUID.randomUUID().toString();
  }


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


  public List<String> getIdList(final Object aKey) {
    List<String> ids = new ArrayList<>();
    Object result = get(aKey);
    if (null == result || !(result instanceof List<?>)) {
      return ids;
    }
    for (Object value : (List<?>) result) {
      if (value instanceof Resource) {
        ids.add(((Resource) value).getAsString(JsonLdConstants.ID));
      }
    }
    return ids;
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


  /**
   * Counts the number of subfields matching the argument string.
   * A simple wildcard ("*") defines 1 level of arbitrary path specifiers.
   * A double wildcard ("**") defines 0-n levels of arbitrary path specifiers.
   * Wildcard string combinations ("*xyz" or "xyz*" etc.) are not supported so far.
   * Arrays can not be specified by position
   * @param aSubfieldPath Specifier for the subfields to be counted.
   * @return The number of specified subfields.
   */
  public Integer getNumberOfSubFields(String aSubfieldPath) {
    String[] pathElements = aSubfieldPath.split("\\.");
    return getNumberOfSubFields(pathElements);
  }

  protected Integer getNumberOfSubFields(String[] aPathElements) {
    int count = 0;
    if (aPathElements.length == 0){
      return count;
    }
    String matchElement = null;
    String pathElement = aPathElements[0];
    String[] remainingElements;
    if (pathElement.equals("**")){
      remainingElements = aPathElements;
      if (aPathElements.length < 3){
        matchElement = remainingElements[remainingElements.length-1];
      }
    }
    else{
      remainingElements = Arrays.copyOfRange(aPathElements, 1, aPathElements.length);
      if (remainingElements.length == 0){
        matchElement = pathElement;
      }
    }
    for (Entry<String, Object> entry : entrySet()) {
      if (entry.getValue() instanceof Resource){
        Resource innerResource = ((Resource) entry.getValue());
        count += innerResource.getNumberOfSubFields(remainingElements);
      } //
      else if (entry.getValue() instanceof List<?>) {
        for (Object innerObject : (List<?>) entry.getValue()) {
          if (innerObject instanceof Resource){
            count += ((Resource)innerObject).getNumberOfSubFields(remainingElements);
          }
        }
      }
      if (entry.getKey().equals(matchElement) || matchElement.equals("**")){
        count++;
      }
    }
    return count;
  }


  protected void fillFromMap(final Map<String, Object> aProperties,
                             final List<String> aIdentifiedTypes) {
    for (Entry<String, Object> entry : aProperties.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (key.equals(JsonLdConstants.ID) && !aIdentifiedTypes.contains(getType())) {
        continue;
      }
      if (value instanceof Map<?, ?>) {
        put(key, new Resource((Map<String, Object>) value));
      } else if (value instanceof List<?>) {
        List<Object> vals = new ArrayList<>();
        for (Object v : (List<?>) value) {
          if (v instanceof Map<?, ?>) {
            vals.add(new Resource((Map<String, Object>) v));
          } else {
            vals.add(v);
          }
        }
        put(key, vals);
      } else {
        put(key, value);
      }
    }
  }


  public ProcessingReport validate(final JsonSchema aSchema) {
    ProcessingReport report = new ListProcessingReport();
    try {
      String type = this.getAsString(JsonLdConstants.TYPE);
      if (null == type) {
        report.error(new ProcessingMessage()
          .setMessage("No type found for ".concat(this.toString()).concat(", cannot validate")));
      } else if (null != getSchemaNode()) {
        report = aSchema.validate(toJson());
      } else {
        Logger.warn("No JSON schema present, validation disabled.");
      }
    } catch (ProcessingException e) {
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
    return OBJECT_MAPPER.convertValue(this, JsonNode.class);
  }

  /**
   * Get a JSON string representation of the resource.
   *
   * @return JSON string
   */
  @Override
  public String toString() {
    ObjectMapper mapper = OBJECT_MAPPER;
    String output;
    try {
      output = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(toJson());
    } catch (JsonProcessingException e) {
      output = toJson().toString();
      e.printStackTrace();
    }
    return output;
  }

  protected abstract JsonNode getSchemaNode();

}
