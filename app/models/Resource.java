package models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import helpers.JsonLdConstants;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import play.Logger;

import javax.validation.constraints.NotNull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;


public class Resource extends HashMap<String, Object> implements Comparable<Resource> {

  /**
   *
   */
  private static final long serialVersionUID = -6177433021348713601L;

  private static final ObjectMapper mObjectMapper = new ObjectMapper();

  // identified ("primary") data types that get an ID
  public static final List<String> mIdentifiedTypes = new ArrayList<>(Arrays.asList(
    "Organization", "Event", "Person", "Action", "WebPage", "Article", "Service", "ConceptScheme",
    "Concept", "Comment", "Product", "LikeAction", "LighthouseAction", "Policy"));

  /**
   * Constructor for typeless resources
   */
  public Resource() {
    this(null);
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
    if (null != aId) {
      this.put(JsonLdConstants.ID, aId);
    } else if (mIdentifiedTypes.contains(aType)) {
      this.put(JsonLdConstants.ID, generateId());
    }
  }

  private static String generateId() {
    return "urn:uuid:" + UUID.randomUUID().toString();
  }

  /**
   * Convert a Map of String/Object to a Resource, assuming that all Object values of the map are
   * properly represented by the toString() method of their class.
   *
   * @param aProperties The map to create the resource from
   * @return a Resource containing all given properties
   */
  @SuppressWarnings("unchecked")
  public static Resource fromMap(Map<String, Object> aProperties) {

    if (aProperties == null) {
      return null;
    }

    Resource resource = new Resource((String) aProperties.get(JsonLdConstants.TYPE),
      (String) aProperties.get(JsonLdConstants.ID));
    resource.putAll(aProperties);
    return resource;

  }

  public static Resource fromJson(JsonNode aJson) {
    Map<String, Object> resourceMap = mObjectMapper.convertValue(aJson,
      new TypeReference<HashMap<String, Object>>() {
      });
    return fromMap(resourceMap);
  }

  public static Resource fromJson(String aJsonString) {
    try {
      return fromJson(mObjectMapper.readTree(aJsonString));
    } catch (IOException e) {
      Logger.error("Could not read resource from JSON", e);
      return null;
    }
  }

  /**
   * Get a JsonNode representation of the resource.
   *
   * @return JSON JsonNode
   */
  public JsonNode toJson() {
    return mObjectMapper.convertValue(this, JsonNode.class);
  }

  /**
   * Get an RDF representation of the resource.
   *
   * @return Model The RDF Model
   */
  public Model toModel() {

    Model model = ModelFactory.createDefaultModel();
    InputStream stream = new ByteArrayInputStream(this.toString().getBytes(StandardCharsets.UTF_8));
    RDFDataMgr.read(model, stream, Lang.JSONLD);
    return model;
  }

  /**
   * Get a JSON string representation of the resource.
   *
   * @return JSON string
   */
  @Override
  public String toString() {
    String output;
    try {
      output = mObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(toJson());
    } catch (JsonProcessingException e) {
      Logger.warn("Could not serialize JSON", e);
      output = toJson().toString();
    }
    return output;
  }

  public String getAsString(final Object aKey) {
    Object result = get(aKey);
    return (result == null) ? null : result.toString();
  }

  public Boolean getAsBoolean(final Object aKey) {
    Object result = get(aKey);
    return (result != null) && ((boolean) result);
  }

  public List<Resource> getAsList(final Object aKey) {
    List<Resource> list = new ArrayList<>();
    Object result = get(aKey);
    if (result instanceof HashMap<?, ?>) {
      list.add(getAsResource(aKey));
    } else if (result instanceof List<?>) {
      for (Object value : (List<?>) result) {
        if (value instanceof HashMap<?, ?>) {
          list.add(Resource.fromMap((HashMap<String, Object>) value));
        }
      }
    }
    return list;
  }

  public Resource getAsResource(final Object aKey) {
    Object result = get(aKey);
    return result instanceof Map<?, ?> ? Resource.fromMap((Map<String, Object>) result) : null;
  }

  Map<?, ?> getAsMap(final String aKey) {
    Object result = get(aKey);
    if (result instanceof Map<?, ?>) {
      return (Map<String, Object>) result;
    }
    return null;
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
      if (pair.getValue() instanceof List<?>) {
        if (!(other.get(pair.getKey()) instanceof List<?>)) {
          return false;
        }
        List<Object> list = (List<Object>) pair.getValue();
        List<Object> otherList = (List<Object>) other.get(pair.getKey());
        if (list.size() != otherList.size() || !list.containsAll(otherList) || !otherList
          .containsAll(list)) {
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

  public String getId() {
    return getAsString(JsonLdConstants.ID);
  }

  public String getType() {
    return getAsString(JsonLdConstants.TYPE);
  }

  public void merge(Resource aOther) {
    for (Entry<String, Object> entry : aOther.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  public Resource reduce() {
    return Resource.fromJson(reduce((ObjectNode) this.toJson()));
  }

  private JsonNode reduce(ObjectNode resource) {
    ObjectNode result = JsonNodeFactory.instance.objectNode();
    Iterator<Map.Entry<String, JsonNode>> fields = resource.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      String key = field.getKey();
      JsonNode value = field.getValue();
      if (value.isObject() && value.has(JsonLdConstants.ID)) {
        ObjectNode link = JsonNodeFactory.instance.objectNode();
        link.set(JsonLdConstants.ID, value.get(JsonLdConstants.ID));
        result.set(key, link);
      } else if (value.isArray()) {
        result.set(key, reduce((ArrayNode) value));
      } else {
        result.set(key, value);
      }
    }
    return result;
  }

  private JsonNode reduce(ArrayNode arrayNode) {
    ArrayNode result = JsonNodeFactory.instance.arrayNode();
    for (JsonNode entry : arrayNode) {
      if (entry.isObject() && entry.has(JsonLdConstants.ID)) {
        ObjectNode link = JsonNodeFactory.instance.objectNode();
        link.set(JsonLdConstants.ID, entry.get(JsonLdConstants.ID));
        result.add(link);
      } else if (entry.isObject()) {
        result.add(reduce((ObjectNode) entry));
      } else if (entry.isArray()) {
        result.add(reduce((ArrayNode) entry));
      } else {
        result.add(entry);
      }
    }
    return result;
  }

  @Override
  public int compareTo(@NotNull Resource aOther) {
    if (hasId() && aOther.hasId()) {
      return getAsString(JsonLdConstants.ID).compareTo(aOther.getAsString(JsonLdConstants.ID));
    }
    return toString().compareTo(aOther.toString());
  }

  public String getNestedFieldValue(final String aNestedKey, final Locale aPreferredLocale) {
    final String[] split = aNestedKey.split("\\.", 2);
    if (split.length == 0) {
      return null;
    }
    if (split.length == 1) {
      Object o = get(split[0]);
      if (o != null) {
        return o.toString();
      }
      return null;
    }
    // split.length == 2
    final Object o = get(split[0]);
    if (o instanceof ArrayList<?>) {
      String next = getNestedValueOfList(split[1], (ArrayList<?>) o, aPreferredLocale);
      if (next != null) {
        return next;
      }
    } else if (o instanceof HashMap<?, ?>) {
      Resource resource = Resource.fromMap((HashMap<String, Object>) o);
      if (resource.size() == 0) {
        return null;
      }
      return resource.getNestedFieldValue(split[1], aPreferredLocale);
    }
    return null;
  }

  private String getNestedValueOfList(final String aKey, final ArrayList<?> aList,
    final Locale aPreferredLocale) {
    Object next;
    final Locale fallbackLocale = Locale.ENGLISH;
    String fallback1 = null;
    String fallback2 = null;
    String fallback3 = null;
    for (Iterator it = aList.iterator(); it.hasNext(); ) {
      next = it.next();
      if (next instanceof HashMap<?, ?>) {
        Resource resource = Resource.fromMap((HashMap<String, Object>) next);
        Object language = resource.get("@language");
        if (language.equals(aPreferredLocale.getLanguage())) {
          return resource.getNestedFieldValue(aKey, aPreferredLocale);
        }
        if (language == null) {
          fallback1 = resource.getNestedFieldValue(aKey, aPreferredLocale);
        } else if (language.equals(fallbackLocale.getLanguage())) {
          fallback2 = resource.getNestedFieldValue(aKey, fallbackLocale);
        } else {
          fallback3 = resource
            .getNestedFieldValue(aKey, Locale.forLanguageTag(language.toString()));
        }
      }
    }
    return (fallback1 != null) ? fallback1 : (fallback2 != null) ? fallback2 : fallback3;
  }

  /**
   * Counts the number of subfields matching the argument string. A simple wildcard ("*") defines 1
   * level of arbitrary path specifiers. A double wildcard ("**") defines 0-n levels of arbitrary
   * path specifiers. Wildcard string combinations ("*xyz" or "xyz*" etc.) are not supported so far.
   * Arrays can not be specified by position
   *
   * @param aSubfieldPath Specifier for the subfields to be counted.
   * @return The number of specified subfields.
   */
  public Integer getNumberOfSubFields(String aSubfieldPath) {
    String[] pathElements = aSubfieldPath.split("\\.");
    return getNumberOfSubFields(pathElements);
  }

  private Integer getNumberOfSubFields(String[] aPathElements) {
    int count = 0;
    if (aPathElements.length == 0) {
      return count;
    }
    String matchElement = null;
    String pathElement = aPathElements[0];
    String[] remainingElements;
    if (pathElement.equals("**")) {
      remainingElements = aPathElements;
      if (aPathElements.length < 3) {
        matchElement = remainingElements[remainingElements.length - 1];
      }
    } else {
      remainingElements = Arrays.copyOfRange(aPathElements, 1, aPathElements.length);
      if (remainingElements.length == 0) {
        matchElement = pathElement;
      }
    }
    for (Entry<String, Object> entry : entrySet()) {
      if (entry.getValue() instanceof HashMap<?, ?>) {
        Resource innerResource = ((Resource.fromMap((HashMap<String, Object>) entry.getValue())));
        count += innerResource.getNumberOfSubFields(remainingElements);
      } else if (entry.getValue() instanceof List<?>) {
        for (Object innerObject : (List<?>) entry.getValue()) {
          if (innerObject instanceof HashMap<?, ?>) {
            count += (Resource.fromMap((HashMap<String, Object>) innerObject))
              .getNumberOfSubFields(remainingElements);
          }
        }
      }
      if (entry.getKey().equals(matchElement) || matchElement.equals("**")) {
        count++;
      }
    }
    return count;
  }

  public Map<String, String> toPointerDict() {
    return jsonNodeToPointerDict(this.toJson(), "");
  }

  private Map<String, String> jsonNodeToPointerDict(JsonNode node, String path) {
    Map<String, String> pointerDict = new HashMap<>();
    if (node.isArray()) {
      for (int i = 0; i < node.size(); i++) {
        pointerDict.putAll(jsonNodeToPointerDict(node.get(i), path.concat("/").concat(Integer.toString(i))));
      }
    } else if (node.isObject()) {
      Iterator<Map.Entry<String, JsonNode>> it = node.fields();
      while (it.hasNext()) {
        Map.Entry<String, JsonNode> entry = it.next();
        pointerDict.putAll(jsonNodeToPointerDict(entry.getValue(), path.concat("/").concat(entry.getKey())));
      }
    } else if (node.isValueNode()) {
      pointerDict.put(path, node.asText());
    }
    return pointerDict;
  }
}
