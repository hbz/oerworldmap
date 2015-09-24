package models;

import com.github.fge.jsonschema.core.report.ProcessingMessage;
import helpers.FilesConfig;
import helpers.JsonLdConstants;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

public class Resource extends HashMap<String, Object> implements Comparable<Resource> {

  /**
   * 
   */
  private static final long serialVersionUID = -6177433021348713601L;

  // identified ("primary") data types that get an ID
  private static final List<String> mIdentifiedTypes = new ArrayList<String>(Arrays.asList(
      "Organization", "Event", "Person", "Action", "WebPage", "Article", "Service", "ConceptScheme", "Concept"));

  public static final String REFERENCEKEY = "referencedBy";

  public Resource() {
  }

  /**
   * Constructor which sets up a random UUID.
   *
   * @param type
   *          The type of the resource.
   */
  public Resource(final String type) {
    this(type, null);
  }

  /**
   * Constructor.
   *
   * @param aType
   *          The type of the resource.
   * @param aId
   *          The id of the resource.
   */
  public Resource(final String aType, final String aId) {
    if (null != aType) {
      this.put(JsonLdConstants.TYPE, aType);
    }
    if (null != aId) {
      this.put(JsonLdConstants.ID, aId);
    } else {
      if (mIdentifiedTypes.contains(aType)) {
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
   * @param aProperties
   *          The map to create the resource from
   * @return a Resource containing all given properties
   */
  @SuppressWarnings("unchecked")
  public static Resource fromMap(Map<String, Object> aProperties) {

    if (aProperties == null) {
      return null;
    }

    String type = (String) aProperties.get(JsonLdConstants.TYPE);
    String id = (String) aProperties.get(JsonLdConstants.ID);
    Resource resource = new Resource(type, id);

    for (Map.Entry<String, Object> entry : aProperties.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (key.equals(JsonLdConstants.ID) && !mIdentifiedTypes.contains(type)) {
        continue;
      }
      if (value instanceof Map<?, ?>) {
        resource.put(key, Resource.fromMap((Map<String, Object>) value));
      } else if (value instanceof List<?>) {
        List<Object> vals = new ArrayList<>();
        for (Object v : (List<?>) value) {
          if (v instanceof Map<?, ?>) {
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

  public static Resource fromJson(String aJsonString) {
    try {
      return fromJson(new ObjectMapper().readTree(aJsonString));
    } catch (IOException e) {
      Logger.error(e.toString());
      return null;
    }
  }

  public ProcessingReport validate() {
    JsonSchema schema;
    ProcessingReport report = new ListProcessingReport();
    try {
      String type = this.getAsString(JsonLdConstants.TYPE);
      if (null == type) {
        report.error(new ProcessingMessage().setMessage("No type found for ".concat(this.toString())
            .concat(", cannot validate")));
      } else {
        schema = JsonSchemaFactory.byDefault().getJsonSchema(
            new ObjectMapper().readTree(Paths.get(FilesConfig.getSchema()).toFile()),
            "/definitions/".concat(type));
        report = schema.validate(toJson());
      }
    } catch (ProcessingException | IOException e) {
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

  public String getAsString(final Object aKey) {
    Object result = get(aKey);
    return (result == null) ? null : result.toString();
  }

  public static Resource getLinkClone(final Resource aResource) {
    if (aResource == null) {
      return null;
    }
    final Resource result = new Resource();
    if (null != aResource.get(JsonLdConstants.ID)) {
      result.put(JsonLdConstants.ID, aResource.get(JsonLdConstants.ID));
    }
    if (null != aResource.get(JsonLdConstants.TYPE)) {
      result.put(JsonLdConstants.TYPE, aResource.get(JsonLdConstants.TYPE));
    }
    if (null != aResource.get("name")) {
      result.put("name", aResource.get("name"));
    }
    return result;
  }

  public static Resource getEmbedClone(final Resource aResource) {
    if (aResource == null) {
      return null;
    }
    final Resource result = new Resource();
    for (Iterator<Map.Entry<String, Object>> it = aResource.entrySet().iterator(); it.hasNext();) {
      Map.Entry<String, Object> entry = it.next();
      // remove entries of type List if they only contain ID entries
      if (entry.getValue() instanceof List) {
        List<?> list = (List<?>) (entry.getValue());
        List<Object> truncatedList = new ArrayList<>();
        for (Iterator<?> innerIt = list.iterator(); innerIt.hasNext();) {
          Object li = innerIt.next();
          if (li instanceof Resource && ((Resource) li).hasId()) {
            truncatedList.add(Resource.getLinkClone((Resource) li));
          } else {
            truncatedList.add(li);
          }
        }
        if (truncatedList.isEmpty()) {
          it.remove();
        }
        result.put(entry.getKey(), truncatedList);
      }
      // remove entries of type Resource if they have an ID
      else if (entry.getValue() instanceof Resource && ((Resource) entry.getValue()).hasId()) {
        result.put(entry.getKey(), getLinkClone((Resource) (entry.getValue())));
      } else {
        result.put(entry.getKey(), entry.getValue());
      }
    }
    return result;
  }

  @SuppressWarnings("unchecked")
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
      if (pair.getValue() instanceof List) {
        if (!(other.get(pair.getKey()) instanceof List)) {
          return false;
        }
        List<Object> list = (List<Object>) pair.getValue();
        List<Object> otherList = (List<Object>) other.get(pair.getKey());
        if (list.size() != otherList.size() || !list.containsAll(otherList)) {
          return false;
        }
      } else if (!pair.getValue().equals(other.get(pair.getKey()))) {
        return false;
      }
    }
    return true;
  }

  public boolean hasId() {
    return containsKey(JsonLdConstants.ID);
  }

  public void merge(Resource aOther) {
    for (Entry<String, Object> entry : aOther.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  public boolean hasIdOnly() {
    if (size() == 1 && hasId()) {
      return true;
    }
    return false;
  }

  public static Resource getFlatClone(Resource aResource) {
    Resource result = Resource.fromMap(aResource);

    for (Entry<String, Object> entry : aResource.entrySet()) {
      if (entry.getValue() instanceof Resource) {
        Resource resource = (Resource) entry.getValue();
        if (resource.hasId()) {
          result.put(entry.getKey(), Resource.getIdClone(resource));
        } //
        else {
          result.put(entry.getKey(), entry.getValue());
        }
      } //
      else if (entry.getValue() instanceof List) {
        result.put(entry.getKey(), getAsIdTree((List<?>) entry.getValue()));
      } else {
        result.put(entry.getKey(), entry.getValue());
      }
    }
    return result;
  }

  private static List<?> getAsIdTree(List<?> aList) {
    for (Object object : aList) {
      if (object instanceof Resource) {
        object = getIdClone((Resource) object);
      } //
      else if (object instanceof List) {
        object = getAsIdTree((List<?>) object);
      }
    }
    return aList;
  }

  private static Resource getIdClone(Resource value) {
    Resource result = new Resource();
    result.put(JsonLdConstants.ID, value.get(JsonLdConstants.ID));
    return result;
  }

  @Override
  public int compareTo(Resource aOther) {
    return getAsString(JsonLdConstants.ID).compareTo(aOther.getAsString(JsonLdConstants.ID));
  }

}
