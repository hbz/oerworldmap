package models;

import helpers.FilesConfig;
import helpers.JsonLdConstants;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.rits.cloning.Cloner;

import helpers.UniversalFunctions;
import play.Logger;
import services.ElasticsearchClient;
import services.ElasticsearchRepository;

public class Resource extends HashMap<String, Object> {

  private static final long serialVersionUID = -6177433021348713601L;

  // identified ("primary") data types that get an ID
  private static final List<String> mIdentifiedTypes = new ArrayList<String>(Arrays.asList(
      "Organization", "Event", "Person", "Action", "WebPage", "Article", "Service"));

  public static final String REFERENCEKEY = "referencedBy";

  public Resource() {
    this(null, null);
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
    if (null != aId){
      this.put(JsonLdConstants.ID, aId);
    }
    else{
      if (mIdentifiedTypes.contains(aType)){
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
   * @param aProperties The map to create the resource from
   * @return a Resource containing all given properties
   */
  @SuppressWarnings("unchecked")
  public static Resource fromMap(Map<String, Object> aProperties) {

    if (aProperties == null){
      return null;
    }

    String type = (String) aProperties.get(JsonLdConstants.TYPE);
    String id = (String) aProperties.get(JsonLdConstants.ID);
    Resource resource = new Resource(type, id);

    for (Map.Entry<String, Object> entry : aProperties.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (key.equals(JsonLdConstants.ID) && ! mIdentifiedTypes.contains(type)) {
        continue;
      }
      if (value instanceof Map) {
        resource.put(key, Resource.fromMap((Map<String, Object>) value));
      } else if (value instanceof List) {
        List<Object> vals = new ArrayList<>();
        for (Object v : (List<?>) value) {
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
    ProcessingReport report;
    try {
      schema = JsonSchemaFactory.byDefault().getJsonSchema(
          new ObjectMapper().readTree(Paths.get(FilesConfig.getSchema()).toFile()),
          "/definitions/".concat(this.get(JsonLdConstants.TYPE).toString()));
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

  @Override
  public boolean containsKey(Object key) {
    String keyString = key.toString();
    if (keyString.startsWith("?")) {
      return keyString.substring(1).equals(this.get(JsonLdConstants.TYPE));
    } else if (keyString.equals("@value")) {
      return super.get("@language") != null
          && super.get("@language").toString().equals(Locale.getDefault().getLanguage());
    }
    return super.containsKey(key);
  }

  @Override
  public Object get(final Object aKey) {
    return get(aKey, false);
  }
  
  public Object get(final Object aKey, final boolean isSuppressEscape) {
    final String keyString = aKey.toString();
    if (keyString.startsWith("?")) {
      return keyString.substring(1).equals(this.get(JsonLdConstants.TYPE));
    } else if (!isSuppressEscape && keyString.equals("email")) {
      return UniversalFunctions.getHtmlEntities(super.get(aKey).toString());
    }
    return super.get(aKey);
  }
  
  public String getAsString(final Object aKey){
    Object result = get(aKey);
    return (result == null) ? null : result.toString();
  }
  
  public static Resource getLinkView(final Resource aResource) {
    final Resource result = new Resource();
    if (null != aResource.get(JsonLdConstants.ID)){
      result.put(JsonLdConstants.ID, aResource.get(JsonLdConstants.ID));
    }
    if (null != aResource.get(JsonLdConstants.TYPE)){
      result.put(JsonLdConstants.TYPE, aResource.get(JsonLdConstants.TYPE));
    }
    if (null != aResource.get("name")){
      result.put("name", aResource.get("name"));
    }
    return result;
  }

  // TODO: the second argument doClone may be unnecessary. Check after implementation of GETting
  // and rePOSTing of denormalized resources.
  public static Resource getEmbedView(final Resource aResource, final boolean doClone) {
    final Resource result;
    if (doClone){
      result = new Cloner().deepClone(aResource); 
    }
    else {
      result = aResource;
    }
    for (Iterator<Map.Entry<String, Object>> it = result.entrySet().iterator(); it.hasNext();) {
      Map.Entry<String, Object> entry = it.next();
      // remove entries of type List if they only contain ID entries
      if (entry.getValue() instanceof List) {
        List<?> list = (List<?>) (entry.getValue());
        List<Object> truncatedList = new ArrayList<>();
        for (Iterator<?> innerIt = list.iterator(); innerIt.hasNext();) {
          Object li = innerIt.next();
          if (li instanceof Resource){
            truncatedList.add(Resource.getLinkView((Resource) li));
          }
          else{
            truncatedList.add(li);
          }
        }
        if (truncatedList.isEmpty()) {
          it.remove();
        }
        result.put(entry.getKey(), truncatedList);
      }
      // remove entries of type Resource if they have an ID
      else if (entry.getValue() instanceof Resource) {
        result.put(entry.getKey(), getLinkView((Resource)(entry.getValue())));
      }
    }
    return result;
  }

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
      if (!pair.getValue().equals(other.get(pair.getKey(), true))) {
        return false;
      }
    }
    return true;
  }

  public boolean hasId() {
    return containsKey(JsonLdConstants.ID);
  }

}
