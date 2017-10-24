package helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.typesafe.config.Config;
import models.Action;
import models.ModelCommon;
import models.Resource;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author pvb
 */
public class Types {

  private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private Map<Class, Type> mTypes;
  private Map<String, String> mIndexTypes;

  public Types(final Config aConfig) throws ProcessingException, IOException {

    mTypes = new HashMap<>();
    mIndexTypes = new HashMap<>();

    JsonNode resourceSchemaNode = OBJECT_MAPPER.readTree(Paths.get(FilesConfig.getResourceSchema()).toFile());
    JsonSchema resourceSchema = JsonSchemaFactory.byDefault().getJsonSchema(resourceSchemaNode);
    Type resourceType = new Type(
      Resource.class,
      "WebPage",
      aConfig.getString("es.index.webpage.name"),
      resourceSchema,
      Resource.getIdentifiedTypes());
    mTypes.put(Resource.class, resourceType);

    JsonNode actionSchemaNode = OBJECT_MAPPER.readTree(Paths.get(FilesConfig.getActionSchema()).toFile());
    JsonSchema actionSchema = JsonSchemaFactory.byDefault().getJsonSchema(actionSchemaNode);
    Type actionType = new Type(
      Action.class,
      "Action",
      aConfig.getString("es.index.action.name"),
      actionSchema,
      Action.getIdentifiedTypes());
    mTypes.put(Action.class, actionType);

    putIndexTypes();
  }

  private void putIndexTypes() {
    for (Map.Entry<Class, Type> type : mTypes.entrySet()){
      for (String subtype : type.getValue().getSubtypes()){
        mIndexTypes.put(subtype, type.getValue().getIndexType());
      }
    }
  }

  private Class getClassByIndexType(final String aIndexType) {
    for (Map.Entry<Class, Type> type : mTypes.entrySet()){
      if (type.getValue().getIndexType().equals(aIndexType)){
        return type.getValue().getClazz();
      }
    }
    return null;
  }

  private String getIndexTypeByType(final String aType){
    return mIndexTypes.get(aType);
  }

  public Class getClassByType(String aType){
    return getClassByIndexType(getIndexTypeByType(aType));
  }

  public String getEsIndexFromClassType(final Class aClass){
    return mTypes.get(aClass).getEsIndex();
  }

  public List<Class> getAllTypeClasses(){
    return Arrays.asList(Resource.class, Action.class);
  }

  public JsonSchema getSchema(final Class aClass) throws IOException{
    return mTypes.get(aClass).mSchema;
  }

  public String getIndexType(Class aClass){
    return mTypes.get(aClass).getIndexType();
  }

  public String getIndexType(ModelCommon aItem) {
    return getIndexType(getClassByType(aItem.getType()));
  }

  public static class Type {

    final private Class mClass;
    final private String mIndexType;
    final private String mEsIndexName;
    final private JsonSchema mSchema;
    final private List<String> mSubtypes;

    public Type(final Class aClass,
                final String aIndexType,
                final String aEsIndex,
                final JsonSchema aSchema,
                final List<String> aSubtypes){
      mClass = aClass;
      mIndexType = aIndexType;
      mEsIndexName = aEsIndex;
      mSchema = aSchema;
      mSubtypes = aSubtypes;
    }

    public Class getClazz(){
      return mClass;
    }

    public String getIndexType(){
      return mIndexType;
    }

    public String getEsIndex() {
      return mEsIndexName;
    }

    public List<String> getSubtypes(){
      return mSubtypes;
    }
  }

}
