package models;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import helpers.FilesConfig;
import helpers.JsonLdConstants;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import play.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;

public class Resource extends ModelCommon implements Comparable<Resource> {

  private static final long serialVersionUID = -6177433021348713601L;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  // identified ("primary") data types that get an ID
  private static final List<String> mIdentifiedTypes = new ArrayList<>(Arrays.asList(
      "Organization", "Event", "Person", "Action", "WebPage", "Article", "Service", "ConceptScheme", "Concept",
    "Comment", "Product"));

  private static JsonNode mSchemaNode = null;

  static {
    try {
      mSchemaNode = OBJECT_MAPPER.readTree(Paths.get(FilesConfig.getResourceSchema()).toFile());
    } catch (IOException e) {
      Logger.error("Could not read resources schema", e);
    }
  }

  /**
   * Constructor which sets up a random UUID.
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
    } else if (mIdentifiedTypes.contains(aType)) {
      this.put(JsonLdConstants.ID, generateId());
    }
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
  public Resource(final Map<String, Object> aProperties) {
    this((String) aProperties.get(JsonLdConstants.TYPE),
      (String) aProperties.get(JsonLdConstants.ID));
    fillFromMap(aProperties, getIdentifiedTypes());
  }

  public Resource(final JsonNode aJson) {
    this((Map<String, Object>) OBJECT_MAPPER.convertValue(aJson,
      new TypeReference<HashMap<String, Object>>() {
      }));
  }

  public Resource(final InputStream aInputStream) throws IOException {
    this(IOUtils.toString(aInputStream, "UTF-8"));
    aInputStream.close();
  }

  public ProcessingReport validate() {
    ProcessingReport report = new ListProcessingReport();
    try {
      String type = this.getAsString(JsonLdConstants.TYPE);
      if (null == type) {
        report.error(new ProcessingMessage()
            .setMessage("No type found for ".concat(this.toString()).concat(", cannot validate")));
      } else if (null != mSchemaNode) {
        JsonSchema schema = JsonSchemaFactory.byDefault().getJsonSchema(mSchemaNode, "/definitions/".concat(type));
        report = schema.validate(toJson());
      } else {
        Logger.warn("No JSON schema present, validation disabled.");
      }
    } catch (ProcessingException e) {
      e.printStackTrace();
    }
    return report;
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
        if (list.size() != otherList.size() || !list.containsAll(otherList) || !otherList.containsAll(list)) {
          return false;
        }
      } else if (!pair.getValue().equals(other.get(pair.getKey()))) {
        return false;
      }
    }
    return true;
  }

  public void merge(Resource aOther) {
    for (Entry<String, Object> entry : aOther.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public int compareTo(Resource aOther) {
    if (hasId() && aOther.hasId()) {
      return getAsString(JsonLdConstants.ID).compareTo(aOther.getAsString(JsonLdConstants.ID));
    }
    return toString().compareTo(aOther.toString());
  }

  /**
   * Get a flat String representation of this Resource, whereby any keys are
   * dismissed. Furthermore, value information can be dropped by specifying its
   * fields respectively keys names. This is useful for "static" values like e.
   * g. of the field "type".
   *
   * @param aFieldSeparator
   *          a String indicating the beginning of new information, resulting
   *          from a new Resource's field. Note, that this separator might also
   *          appear within the Resource's fields themselves.
   * @param aDropFields
   *          a List<String> specifying, which field's values should be excluded
   *          from the resulting String representation
   * @return a flat String representation of this Resource. In case there is no
   *         information to be returned, the result is an empty String.
   */
  public String getValuesAsFlatString(String aFieldSeparator, List<String> aDropFields) {
    String fieldSeparator = null == aFieldSeparator ? "" : aFieldSeparator;

    StringBuffer result = new StringBuffer();

    for (Entry<String, Object> entry : entrySet()) {
      if (!aDropFields.contains(entry.getKey())) {
        Object value = entry.getValue();
        if (value instanceof String) {
          if (!"".equals(value)) {
            result.append(value).append(fieldSeparator).append(" ");
          }
        } //
        else if (value instanceof Resource) {
          result.append(((Resource) value).getValuesAsFlatString(fieldSeparator, aDropFields));
        } //
        else if (value instanceof List<?>) {
          result.append("[");
          for (Object innerValue : (List<?>) value) {
            if (innerValue instanceof String) {
              if (!"".equals(innerValue)) {
                result.append(innerValue).append(fieldSeparator).append(" ");
              }
            } //
            else if (innerValue instanceof Resource) {
              result.append(
                  ((Resource) innerValue).getValuesAsFlatString(fieldSeparator, aDropFields));
            }
          }
          if (result.length() > 1 && result.charAt(result.length() - 1) != '[') {
            result.delete(result.length() - 2, result.length());
          }
          result.append("]").append(fieldSeparator).append(" ");
        }
      }
    }
    // delete last separator
    if (result.length() > 1) {
      result.delete(result.length() - 2, result.length());
    }
    return result.toString();
  }


  @Override
  protected JsonNode getSchemaNode(){
    return mSchemaNode;
  }

  public static List<String> getIdentifiedTypes(){
    return mIdentifiedTypes;
  }
}
