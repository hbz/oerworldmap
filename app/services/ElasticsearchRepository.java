package services;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import java.util.UUID;

import java.io.IOException;


import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;

import models.Resource;
import helpers.JsonLdConstants;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import play.api.libs.json.Json;

public class ElasticsearchRepository implements ResourceRepository {

  final private ElasticsearchClient elasticsearch;

  final public static String DEFAULT_TYPE = "resource";
  final public static String AGGREGATION_TYPE = "aggregation";

  public ElasticsearchRepository(@Nonnull ElasticsearchClient aElasticsearchClient) {
    elasticsearch = aElasticsearchClient;
  }

  @Override
  public void addResource(Resource aResource) throws IOException {
    String id = (String) aResource.get(JsonLdConstants.ID);
    String type = (String) aResource.get(JsonLdConstants.TYPE);
    if (StringUtils.isEmpty(id)){
      id = UUID.randomUUID().toString();
    }
    if (StringUtils.isEmpty(type)){
      type = DEFAULT_TYPE;
    }
    elasticsearch.addJson(aResource.toString(), id, type);
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

  public List<Resource> query(AggregationBuilder aAggregationBuilder) throws IOException {
    List<Resource> resources = new ArrayList<Resource>();
    Map<String, Object>  doc = elasticsearch.getAggregation(aAggregationBuilder);
    doc.put(JsonLdConstants.TYPE, AGGREGATION_TYPE);
    resources.add(Resource.fromMap(doc));
    return resources;
  }

}
