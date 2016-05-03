package services.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.index.query.AndFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.GeoBoundingBoxFilterBuilder;
import org.elasticsearch.index.query.GeoPolygonFilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.json.simple.parser.ParseException;

import com.typesafe.config.Config;

import helpers.JsonLdConstants;
import models.Record;
import models.Resource;
import models.ResourceList;
import play.Logger;
import services.ElasticsearchConfig;
import services.QueryContext;

public class ElasticsearchRepository extends Repository implements Readable, Writable, Queryable, Aggregatable {

  private static ElasticsearchConfig mConfig;
  private Client mClient;

  public ElasticsearchRepository(Config aConfiguration) {
    super(aConfiguration);
    mConfig = new ElasticsearchConfig(aConfiguration);
    mClient = mConfig.getClient();
  }

  @Override
  public void addResource(@Nonnull final Resource aResource, @Nonnull final String aType) throws IOException {
    String id = (String) aResource.getId();
    if (StringUtils.isEmpty(id)) {
      id = UUID.randomUUID().toString();
    }
    addJson(aResource.toString(), id, aType);
    refreshIndex(mConfig.getIndex());
  }

  public void refreshIndex(String aIndex) {
    try {
      mClient.admin().indices().refresh(new RefreshRequest(aIndex)).actionGet();
    } catch (IndexMissingException e) {
      Logger.error("Trying to refresh index \"" + aIndex + "\" in Elasticsearch.");
      e.printStackTrace();
    }
  }

  private void addJson(final String aJsonString, final String aUuid, final String aType) {
    mClient.prepareIndex(mConfig.getIndex(), aType, aUuid).setSource(aJsonString).execute().actionGet();
  }

  @Override
  public Resource getResource(@Nonnull String aId) {
    return Resource.fromMap(getDocument("_all", aId));
  }

  public List<Resource> getResources(@Nonnull String aField, @Nonnull Object aValue) {
    List<Resource> resources = new ArrayList<Resource>();

    final int docsPerPage = 1024;
    int count = 0;
    SearchResponse response = null;
    final List<Map<String, Object>> docs = new ArrayList<>();
    while (response == null || response.getHits().hits().length != 0) {
      response = mClient.prepareSearch(mConfig.getIndex())
          .setQuery(QueryBuilders.queryString(aField.concat(":").concat(QueryParser.escape(aValue.toString()))))
          .setSize(docsPerPage).setFrom(count * docsPerPage).execute().actionGet();
      for (SearchHit hit : response.getHits()) {
        docs.add(hit.getSource());
      }
      count++;
    }

    for (Map<String, Object> doc : docs) {
      resources.add(Resource.fromMap(doc));
    }
    return resources;
  }

  @Override
  public List<Resource> getAll(@Nonnull String aType) throws IOException {
    List<Resource> resources = new ArrayList<Resource>();
    final int docsPerPage = 1024;
    int count = 0;
    SearchResponse response = null;
    final List<Map<String, Object>> docs = new ArrayList<Map<String, Object>>();
    while (response == null || response.getHits().hits().length != 0) {
      response = mClient.prepareSearch(mConfig.getIndex()).setTypes(aType).setQuery(QueryBuilders.matchAllQuery())
          .setSize(docsPerPage).setFrom(count * docsPerPage).execute().actionGet();
      for (SearchHit hit : response.getHits()) {
        docs.add(hit.getSource());
      }
      count++;
    }
    for (Map<String, Object> doc : docs) {
      resources.add(Resource.fromMap(doc));
    }
    return resources;
  }

  @Override
  public Resource deleteResource(@Nonnull String aId) {
    Resource resource = getResource(aId);
    if (null == resource) {
      return null;
    }
    String type = ((Resource) resource.get(Record.RESOURCEKEY)).get(JsonLdConstants.TYPE).toString();
    Logger.info("DELETING " + type + aId);

    boolean found = deleteDocument(type, aId);
    refreshIndex(mConfig.getIndex());
    if (found) {
      return resource;
    } else {
      return null;
    }
  }

  @Override
  public Resource aggregate(@Nonnull AggregationBuilder<?> aAggregationBuilder) throws IOException {
    return aggregate(aAggregationBuilder, null);
  }

  public Resource aggregate(@Nonnull AggregationBuilder<?> aAggregationBuilder, QueryContext aQueryContext)
      throws IOException {
    Resource aggregations = Resource.fromJson(getAggregation(aAggregationBuilder, aQueryContext).toString());
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
   * @param aQueryString
   *          A string describing the query
   * @param aFilters
   * @return A resource resembling the result set of resources matching the
   *         criteria given in the query string
   * @throws IOException
   * @throws ParseException
   */
  @Override
  public ResourceList query(@Nonnull String aQueryString, int aFrom, int aSize, String aSortOrder,
      Map<String, ArrayList<String>> aFilters) throws IOException, ParseException {
    return query(aQueryString, aFrom, aSize, aSortOrder, aFilters, null);
  }

  public ResourceList query(@Nonnull String aQueryString, int aFrom, int aSize, String aSortOrder,
      Map<String, ArrayList<String>> aFilters, QueryContext aQueryContext) throws IOException, ParseException {

    SearchResponse response = esQuery(aQueryString, aFrom, aSize, aSortOrder, aFilters, aQueryContext);
    Iterator<SearchHit> searchHits = response.getHits().iterator();
    List<Resource> matches = new ArrayList<>();
    while (searchHits.hasNext()) {
      Resource match = Resource.fromMap(searchHits.next().sourceAsMap());
      matches.add(match);
    }
    Resource aAggregations = (Resource) Resource.fromJson(response.toString()).get("aggregations");
    return new ResourceList(matches, response.getHits().getTotalHits(), aQueryString, aFrom, aSize, aSortOrder,
        aFilters, aAggregations);

  }

  private SearchResponse getAggregation(final AggregationBuilder<?> aAggregationBuilder, QueryContext aQueryContext) {

    SearchRequestBuilder searchRequestBuilder = mClient.prepareSearch(mConfig.getIndex());

    AndFilterBuilder globalAndFilter = FilterBuilders.andFilter();
    if (!(null == aQueryContext)) {
      for (FilterBuilder contextFilter : aQueryContext.getFilters()) {
        globalAndFilter.add(contextFilter);
      }
    }

    SearchResponse response = searchRequestBuilder.addAggregation(aAggregationBuilder)
        .setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), globalAndFilter)).setSize(0).execute()
        .actionGet();
    return response;

  }

  private Map<String, Object> getDocument(@Nonnull final String aType, @Nonnull final String aIdentifier) {
    final GetResponse response = mClient.prepareGet(mConfig.getIndex(), aType, aIdentifier).execute().actionGet();
    return response.getSource();
  }

  public Map<String, Object> getDocument(@Nonnull final String aType, @Nonnull final UUID aUuid) {
    return getDocument(aType, aUuid.toString());
  }

  private boolean deleteDocument(@Nonnull final String aType, @Nonnull final String aIdentifier) {
    final DeleteResponse response = mClient.prepareDelete(mConfig.getIndex(), aType, aIdentifier).execute().actionGet();
    return response.isFound();
  }

  public boolean hasIndex(String aIndex) {
    return mClient.admin().indices().prepareExists(aIndex).execute().actionGet().isExists();
  }

  public void deleteIndex(String aIndex) {
    try {
      mClient.admin().indices().delete(new DeleteIndexRequest(aIndex)).actionGet();
    } catch (IndexMissingException e) {
      Logger.error("Trying to delete index \"" + aIndex + "\" from Elasticsearch.");
      e.printStackTrace();
    }
  }

  public void createIndex(String aIndex) {
    try {
      mClient.admin().indices().prepareCreate(aIndex).setSource(mConfig.getIndexConfigString()).execute().actionGet();
      mClient.admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet();
    } catch (ElasticsearchException indexAlreadyExists) {
      Logger.error("Trying to create index \"" + aIndex + "\" in Elasticsearch. Index already exists.");
      indexAlreadyExists.printStackTrace();
    } catch (IOException ioException) {
      Logger.error("Trying to create index \"" + aIndex + "\" in Elasticsearch. Couldn't read index config file.");
      ioException.printStackTrace();
    }
  }

  private SearchResponse esQuery(@Nonnull String aQueryString, int aFrom, int aSize, String aSortOrder,
      Map<String, ArrayList<String>> aFilters, QueryContext aQueryContext) {

    SearchRequestBuilder searchRequestBuilder = mClient.prepareSearch(mConfig.getIndex());

    AndFilterBuilder globalAndFilter = FilterBuilders.andFilter();

    String[] fieldBoosts = null;

    if (!(null == aQueryContext)) {
      searchRequestBuilder.setFetchSource(aQueryContext.getFetchSource(), null);
      for (FilterBuilder contextFilter : aQueryContext.getFilters()) {
        globalAndFilter.add(contextFilter);
      }
      for (AggregationBuilder<?> contextAggregation : aQueryContext.getAggregations()) {
        searchRequestBuilder.addAggregation(contextAggregation);
      }
      if (aQueryContext.hasFieldBoosts()) {
        fieldBoosts = aQueryContext.getElasticsearchFieldBoosts();
      }
      if (null != aQueryContext.getZoomTopLeft() && null != aQueryContext.getZoomBottomRight()) {
        GeoBoundingBoxFilterBuilder zoomFilter = FilterBuilders.geoBoundingBoxFilter("about.location.geo")//
            .topLeft(aQueryContext.getZoomTopLeft())//
            .bottomRight(aQueryContext.getZoomBottomRight());
        globalAndFilter.add(zoomFilter);
      }
      if (null != aQueryContext.getPolygonFilter() && !aQueryContext.getPolygonFilter().isEmpty()){
    	GeoPolygonFilterBuilder polygonFilter = FilterBuilders.geoPolygonFilter("about.location.geo");
        for (GeoPoint geoPoint : aQueryContext.getPolygonFilter()){
          polygonFilter.addPoint(geoPoint);
        }
    	globalAndFilter.add(polygonFilter);
      }
    }

    if (!StringUtils.isEmpty(aSortOrder)) {
      String[] sort = aSortOrder.split(":");
      if (2 == sort.length) {
        searchRequestBuilder.addSort(sort[0], sort[1].toUpperCase().equals("ASC") ? SortOrder.ASC : SortOrder.DESC);
      } else {
        Logger.error("Invalid sort string: " + aSortOrder);
      }
    }

    if (!(null == aFilters)) {
      AndFilterBuilder aggregationAndFilter = FilterBuilders.andFilter();
      for (Map.Entry<String, ArrayList<String>> entry : aFilters.entrySet()) {
        // This could also be an OrFilterBuilder allowing to expand the result
        // list
        AndFilterBuilder andTermFilterBuilder = FilterBuilders.andFilter();
        for (String filter : entry.getValue()) {
          andTermFilterBuilder.add(FilterBuilders.termFilter(entry.getKey(), filter));
        }
        aggregationAndFilter.add(andTermFilterBuilder);
      }
      globalAndFilter.add(aggregationAndFilter);
    }

    QueryBuilder queryBuilder;
    if (!StringUtils.isEmpty(aQueryString)) {
      queryBuilder = QueryBuilders.queryString(aQueryString);
      if (fieldBoosts != null) {
        for (String fieldBoost : fieldBoosts) {
          try {
            ((QueryStringQueryBuilder) queryBuilder).field(fieldBoost.split("\\^")[0],
                Float.parseFloat(fieldBoost.split("\\^")[1]));
          } catch (ArrayIndexOutOfBoundsException e) {
            Logger.error("Invalid field boost: " + fieldBoost);
          }
        }
      }
    } else {
      queryBuilder = QueryBuilders.matchAllQuery();
    }

    searchRequestBuilder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
        .setQuery(QueryBuilders.filteredQuery(queryBuilder, globalAndFilter));

    return searchRequestBuilder.setFrom(aFrom).setSize(aSize).execute().actionGet();

  }
}
