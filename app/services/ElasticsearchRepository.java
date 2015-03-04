package services;

import helpers.JsonLdConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import models.Resource;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.json.simple.parser.ParseException;

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
    if (StringUtils.isEmpty(id)) {
      id = UUID.randomUUID().toString();
    }
    if (StringUtils.isEmpty(type)) {
      type = DEFAULT_TYPE;
    }
    elasticsearch.addJson(aResource.toString(), id, type);
  }

  @Override
  public Resource getResource(String aId) throws IOException {
    // FIXME: results in NullPointerException if aId is unknown
    return Resource.fromMap(elasticsearch.getDocument("_all", aId));
  }

  /**
   * Get a (Linked) List of Resources that are of the specified type and have
   * the specified content in that specified field.
   *
   * @param aType
   * @param aField
   * @param aContent
   * @return all matching Resources or an empty list if no resources match the
   *         given field / content combination.
   */
  public List<Resource> getResourcesByContent(@Nonnull String aType, @Nonnull String aField, String aContent) {
    if (StringUtils.isEmpty(aType) || StringUtils.isEmpty(aField)) {
      throw new IllegalArgumentException("Non-complete arguments.");
    } else {
      List<Resource> resources = new LinkedList<Resource>();
      List<Map<String, Object>> maps = elasticsearch.getExactMatches(aType, aField, aContent);
      if (maps != null) {
        for (Map<String, Object> map : maps) {
          resources.add(Resource.fromMap(map));
        }
      }
      return resources;
    }
  }

  @Override
  public List<Resource> query(String aType) throws IOException {
    List<Resource> resources = new ArrayList<Resource>();
    for (Map<String, Object> doc : elasticsearch.getAllDocs(aType)) {
      resources.add(Resource.fromMap(doc));
    }
    return resources;
  }

  public Resource query(AggregationBuilder aAggregationBuilder) throws IOException {
    Resource aggregation = new Resource("Aggregation", "country-list");
    aggregation.put("entries", elasticsearch.getAggregation(aAggregationBuilder));
    return aggregation;
  }

  /**
   * This search method is designed to be able to make use of the complete
   * Elasticsearch query syntax, as described in
   * http://www.elasticsearch.org/guide
   * /en/elasticsearch/reference/current/search-uri-request.html .
   *
   * @param aEsQuery a String representing the
   * @return an ArrayList of Resources representing the search result
   * @throws IOException
   * @throws ParseException
   */
  public List<Resource> esQuery(String aEsQuery) throws IOException, ParseException {
    List<Resource> resources = new ArrayList<Resource>();
    for (Map<String, Object> doc : elasticsearch.esQuery(aEsQuery)) {
      resources.add(Resource.fromMap(doc));
    }
    return resources;
  }

}
