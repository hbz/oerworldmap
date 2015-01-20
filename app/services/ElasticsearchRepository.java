package services;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;

import models.Resource;
import helpers.JsonLdConstants;

public class ElasticsearchRepository implements ResourceRepository {

  final private ElasticsearchClient elasticsearch;

  public ElasticsearchRepository() {
    elasticsearch = new ElasticsearchClient();
  }

  public ElasticsearchRepository(@Nonnull ElasticsearchClient aElasticsearchClient) {
    elasticsearch = aElasticsearchClient;
  }

  @Override
  public void addResource(Resource aResource) {
    String id = (String) aResource.get(JsonLdConstants.ID);
    if (StringUtils.isEmpty(id)){
      id = UUID.randomUUID().toString();
    }
    elasticsearch.addJson(aResource.toString(), id);
  }

  @Override
  public List<Resource> queryAll(String aType) {
    List<Resource> resources = new ArrayList<Resource>();
    for (Map<String, Object> doc : elasticsearch.getAllDocs(aType)) {
      resources.add(fromMap(doc));
    }
    return resources;
  }

  @Override
  public Resource query(String aType, String aId) {
    return fromMap(elasticsearch.getDocument(aType, aId));
  }

  /**
   * Convert a Map of String/Object to a Resource, assuming that all Object
   * values of the map are properly represented by the toString() method of
   * their class.
   * 
   * @param aProperties
   * @return a Resource containing all given properties
   */
  private Resource fromMap(Map<String, Object> aProperties) {
    checkTypeExistence(aProperties);
    Resource resource;
    if (hasId(aProperties)) {
      resource = new Resource((String) aProperties.get(JsonLdConstants.TYPE),
                              (String) aProperties.get(JsonLdConstants.ID));
    } else {
      resource = new Resource((String) aProperties.get(JsonLdConstants.TYPE));
    }
    Iterator<Entry<String, Object>> it = aProperties.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, Object> pair = (Map.Entry<String, Object>) it.next();
      if (resource.isSetable(pair.getKey())){
        resource.set(pair.getKey(), pair.getValue().toString());
      }
      it.remove();
    }
    return resource;
  }

  private boolean hasId(Map<String, Object> aProperties) {
    return !StringUtils.isEmpty(aProperties.get(JsonLdConstants.ID).toString());
  }

  private void checkTypeExistence(Map<String, Object> aProperties) {
    Object type = aProperties.get(JsonLdConstants.TYPE);
    if (!(type instanceof String) || StringUtils.isEmpty((String) type)) {
      String message = "Unspecified " + JsonLdConstants.TYPE + " : " + aProperties.hashCode();
      System.err.println(message);
      try {
        throw new java.lang.TypeNotPresentException(message, new Exception());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
