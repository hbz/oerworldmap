package services;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.io.IOException;

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
  public void addResource(Resource aResource) throws IOException {
    elasticsearch.addJson(aResource.toString());
  }

  @Override
  public Resource getResource(String aId) throws IOException {
    return Resource.fromMap(elasticsearch.getDocument("_all", aId));
  }

  @Override
  public List<Resource> query(String aType) throws IOException {
    List<Resource> resources = new ArrayList<Resource>();
    for (Map<String, Object> doc : elasticsearch.getAllDocs(aType)){
      resources.add(Resource.fromMap(doc));
    }
    return resources;
  }

}
