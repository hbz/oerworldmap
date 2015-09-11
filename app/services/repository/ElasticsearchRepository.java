package services.repository;

import helpers.JsonLdConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import models.Record;
import models.Resource;

import models.ResourceList;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.json.simple.parser.ParseException;
import play.Logger;
import services.ElasticsearchProvider;

public class ElasticsearchRepository implements Readable, Writable, Queryable, Aggregatable {

  final private ElasticsearchProvider elasticsearch;

  final public static String DEFAULT_TYPE = "Thing";

  public ElasticsearchRepository(@Nonnull ElasticsearchProvider aElasticsearchProvider) {
    elasticsearch = aElasticsearchProvider;
  }

  @Override
  public void addResource(@Nonnull final Resource aResource, @Nonnull final String aType)
      throws IOException {
    String id = (String) aResource.get(JsonLdConstants.ID);
    if (StringUtils.isEmpty(id)) {
      id = UUID.randomUUID().toString();
    }
    elasticsearch.addJson(aResource.toString(), id, aType);
  }

  @Override
  public Resource getResource(String aId) {
    return Resource.fromMap(elasticsearch.getDocument("_all", aId));
  }

  @Override
  public List<Resource> getAll(String aType) throws IOException {
    List<Resource> resources = new ArrayList<Resource>();
    for (Map<String, Object> doc : elasticsearch.getAllDocs(aType)) {
      resources.add(Resource.fromMap(doc));
    }
    return resources;
  }


  @Override
  public Resource deleteResource(String aId) {
    // TODO: delete mentioned resources?
    Resource resource = getResource(aId);
    if (null == resource) {
      return null;
    }

    // FIXME: check why deleting from _all is not possible, remove dependency on
    // Record class
    String type = ((Resource) resource.get(Record.RESOURCEKEY)).get(JsonLdConstants.TYPE)
        .toString();
    Logger.info("DELETING " + type + aId);

    if (elasticsearch.deleteDocument(type, aId)) {
      return resource;
    } else {
      return null;
    }
  }

  @Override
  public Resource aggregate(AggregationBuilder<?> aAggregationBuilder) throws IOException {
    Resource aggregations = Resource.fromJson(elasticsearch.getAggregation(aAggregationBuilder)
        .toString());
    if (null == aggregations) {
      return null;
    }
    return (Resource) aggregations.get("aggregations");
  }

  /**
   * This search method is designed to be able to make use of the complete
   * Elasticsearch query syntax, as described in
   * http://www.elasticsearch.org/guide
   * /en/elasticsearch/reference/current/search-uri-request.html .
   *
   * @param  aQueryString A string describing the query
   * @return A resource resembling the result set of resources matching the criteria given in the query string
   * @throws IOException
   * @throws ParseException
   */
  @Override
  public ResourceList query(@Nonnull String aQueryString, String aSortOrder) throws IOException, ParseException {
    ResourceList resources = new ResourceList();
    for (Map<String, Object> doc : elasticsearch.esQuery(aQueryString, null, aSortOrder)) {
      resources.add(Resource.fromMap(doc));
    }
    return resources;
  }
}
