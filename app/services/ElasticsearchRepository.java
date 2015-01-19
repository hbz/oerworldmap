package services;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

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
    elasticsearch.addJson(aResource.toString());
  }

  @Override
  public Resource getResource(String aId) {
    return fromMap(elasticsearch.getDocument("_all", aId));
  }

  @Override
  public List<Resource> query(String aType) {
    List<Resource> resources = new ArrayList<Resource>();
    for (Map<String, Object> doc : elasticsearch.getAllDocs(aType)){
      resources.add(fromMap(doc));
    }
    return resources;
  }

  /**
   * Convert a Map of String/Object to a Resource, assuming that all
   * Object values of the map are properly represented by the toString()
   * method of their class.
   * @param aProperties
   * @return a Resource containing all given properties
   */
  private Resource fromMap(Map<String, Object> aProperties) {
    Resource resource = new Resource((String)aProperties.get(JsonLdConstants.ID));
    Iterator<Entry<String, Object>> it = aProperties.entrySet().iterator();
    while (it.hasNext()) {
        Map.Entry<String, Object> pair = (Map.Entry<String, Object>)it.next();
        resource.set(pair.getKey(), pair.getValue().toString());
        it.remove();
    }
    return resource;
  }

}
