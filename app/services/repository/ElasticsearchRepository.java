package services.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;
import helpers.JsonLdConstants;
import models.Record;
import models.Resource;
import models.ResourceList;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.lucene.search.function.FiltersFunctionScoreQuery;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import play.Logger;
import services.ElasticsearchConfig;
import services.QueryContext;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class ElasticsearchRepository extends Repository implements Readable, Writable, Queryable, Aggregatable {

  private static ElasticsearchConfig mConfig;
  private Client mClient;
  private Fuzziness mFuzziness;
  private static JsonNodeFactory mJsonNodeFactory = new JsonNodeFactory(false);

  public ElasticsearchRepository(Config aConfiguration) {
    super(aConfiguration);
    mConfig = new ElasticsearchConfig(aConfiguration);
    Settings settings = Settings.settingsBuilder().put(mConfig.getClientSettings()).build();
    try {
      mClient = TransportClient.builder().settings(settings).build().addTransportAddress(
        new InetSocketTransportAddress(InetAddress.getByName(mConfig.getServer()), mConfig.getJavaPort()));
    } catch (UnknownHostException ex) {
      throw new RuntimeException(ex);
    }
    mFuzziness = mConfig.getFuzziness();
  }

  @Override
  public void addResource(@Nonnull final Resource aResource, Map<String, String> aMetadata) throws IOException {
    Record record = new Record(aResource);
    for (String key : aMetadata.keySet()) {
      record.put(key, aMetadata.get(key));
    }
    addJson(record.toString(), record.getId(), Record.TYPE);
    refreshIndex(mConfig.getIndex());
  }

  @Override
  public void addResources(@Nonnull List<Resource> aResources, Map<String, String> aMetadata) throws IOException {
    Map<String, String> records = new HashMap<>();
    for (Resource resource : aResources) {
      Record record = new Record(resource);
      for (String key : aMetadata.keySet()) {
        record.put(key, aMetadata.get(key));
      }
      records.put(record.getId(), record.toString());
    }
    addJson(records, Record.TYPE);
    refreshIndex(mConfig.getIndex());
  }

  @Override
  public Resource getResource(@Nonnull String aId) {
    return Resource.fromMap(getDocument(Record.TYPE, aId));
  }

  public List<Resource> getResources(@Nonnull String aField, @Nonnull Object aValue) {
    List<Resource> resources = new ArrayList<>();
    for (Map<String, Object> doc : getDocuments(aField, aValue)) {
      resources.add(Resource.fromMap(doc));
    }
    return resources;
  }

  @Override
  public List<Resource> getAll(@Nonnull String aType) throws IOException {
    List<Resource> resources = new ArrayList<>();
    for (Map<String, Object> doc : getDocuments(Record.RESOURCE_KEY.concat(".")
      .concat(JsonLdConstants.TYPE), aType)) {
      resources.add(Resource.fromMap(doc));
    }
    return resources;
  }

  @Override
  public Resource deleteResource(@Nonnull String aId, Map<String, String> aMetadata) {
    Resource resource = getResource(aId.concat(".").concat(Record.RESOURCE_KEY));
    if (null == resource) {
      return null;
    }
    boolean found = deleteDocument(Record.TYPE, resource.getId());
    refreshIndex(mConfig.getIndex());
    Logger.trace("Deleted " + aId + " from Elasticsearch");
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

  public Resource aggregate(@Nonnull AggregationBuilder<?> aAggregationBuilder, QueryContext aQueryContext) {
    Resource aggregations = Resource
      .fromJson(getAggregation(aAggregationBuilder, aQueryContext).toString());
    if (null == aggregations) {
      return null;
    }
    return (Resource) aggregations.get("aggregations");
  }

  public Resource aggregate(@Nonnull List<AggregationBuilder<?>> aAggregationBuilders, QueryContext aQueryContext) {
    Resource aggregations = Resource
      .fromJson(getAggregations(aAggregationBuilders, aQueryContext).toString());
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
   */
  @Override
  public ResourceList query(@Nonnull String aQueryString, int aFrom, int aSize, String aSortOrder,
                            Map<String, List<String>> aFilters) throws IOException {
    return query(aQueryString, aFrom, aSize, aSortOrder, aFilters, null);
  }

  public ResourceList query(@Nonnull String aQueryString, int aFrom, int aSize, String aSortOrder,
                            Map<String, List<String>> aFilters, QueryContext aQueryContext) throws IOException {

    return esQuery(aQueryString, aFrom, aSize, aSortOrder, aFilters, aQueryContext, false);

  }

  public JsonNode reconcile(@Nonnull String aQuery, int aFrom, int aSize, String aSortOrder,
                            Map<String, List<String>> aFilters, QueryContext aQueryContext,
                            final Locale aPreferredLocale) {

    aQueryContext.setFetchSource(new String[]{"about.@id", "about.@type", "about.name"});

    ResourceList response = esQuery(aQuery, aFrom, aSize, aSortOrder, aFilters, aQueryContext, true);
    Iterator<Resource> searchHits = response.getItems().iterator();
    ArrayNode resultItems = new ArrayNode(mJsonNodeFactory);

    while (searchHits.hasNext()) {
      final Resource hit = searchHits.next();
      Resource match = hit.getAsResource(Record.RESOURCE_KEY);
      String name = match.getNestedFieldValue("name.@value", aPreferredLocale);
      ObjectNode item = new ObjectNode(mJsonNodeFactory);
      item.put("id", match.getId());
      item.put("match", aQuery.toLowerCase().replaceAll("[ ,\\.\\-_+]", "")
        .equals(name.toLowerCase().replaceAll("[ ,\\.\\-_+]", "")));
      item.put("name", name);
      item.put("score", hit.getAsString("_score"));
      ArrayNode typeArray = new ArrayNode(mJsonNodeFactory);
      typeArray.add(match.getType());
      item.set("type", typeArray);
      resultItems.add(item);
    }

    ObjectNode result = new ObjectNode(mJsonNodeFactory);
    result.set("result", resultItems);
    return result;
  }

  /**
   * Add a document consisting of a JSON String specified by a given UUID and a
   * given type.
   *
   * @param aJsonString
   */
  public void addJson(final String aJsonString, final String aUuid, final String aType) {
    mClient.prepareIndex(mConfig.getIndex(), aType, aUuid).setSource(aJsonString).execute()
      .actionGet();
  }

  /**
   * Add documents consisting of JSON Strings specified by a given UUID and a
   * given type.
   *
   * @param aJsonStringIdMap
   */
  public void addJson(final Map<String, String> aJsonStringIdMap, final String aType) {

    BulkRequestBuilder bulkRequest = mClient.prepareBulk();
    for (Map.Entry<String, String> entry : aJsonStringIdMap.entrySet()) {
      String id = entry.getKey();
      String json = entry.getValue();
      bulkRequest.add(mClient.prepareIndex(mConfig.getIndex(), aType, id).setSource(json));
    }

    BulkResponse bulkResponse = bulkRequest.execute().actionGet();
    if (bulkResponse.hasFailures()) {
      Logger.error(bulkResponse.buildFailureMessage());
    }

  }

  private List<Map<String, Object>> getDocuments(final String aField, final Object aValue) {
    final int docsPerPage = 1024;
    int count = 0;
    SearchResponse response = null;
    final List<Map<String, Object>> docs = new ArrayList<>();
    while (response == null || response.getHits().hits().length != 0) {
      response = mClient.prepareSearch(mConfig.getIndex())
        .setQuery(QueryBuilders.queryStringQuery(aField.concat(":").concat(QueryParser.escape(aValue.toString()))))
        .setSize(docsPerPage).setFrom(count * docsPerPage).execute().actionGet();
      for (SearchHit hit : response.getHits()) {
        docs.add(hit.getSource());
      }
      count++;
    }
    return docs;
  }

  /**
   * Get a document of a specified type specified by an identifier.
   *
   * @param aType
   * @param aIdentifier
   * @return the document as Map of String/Object
   */
  private Map<String, Object> getDocument(@Nonnull final String aType,
                                         @Nonnull final String aIdentifier) {
    final GetResponse response = mClient.prepareGet(mConfig.getIndex(), aType, aIdentifier)
      .execute().actionGet();
    return response.getSource();
  }

  private boolean deleteDocument(@Nonnull final String aType, @Nonnull final String aIdentifier) {
    final DeleteResponse response = mClient.prepareDelete(mConfig.getIndex(), aType, aIdentifier)
      .execute().actionGet();
    return response.isFound();
  }

  private SearchResponse getAggregation(final AggregationBuilder<?> aAggregationBuilder, QueryContext aQueryContext) {

    SearchRequestBuilder searchRequestBuilder = mClient.prepareSearch(mConfig.getIndex());

    BoolQueryBuilder globalAndFilter = QueryBuilders.boolQuery();

    if (!(null == aQueryContext)) {
      for (QueryBuilder contextFilter : aQueryContext.getFilters()) {
        globalAndFilter.must(contextFilter);
      }
    }

    SearchResponse response = searchRequestBuilder.addAggregation(aAggregationBuilder)
      .setQuery(QueryBuilders.boolQuery().filter(globalAndFilter))
      .setSize(0).execute().actionGet();
    return response;

  }

  private SearchResponse getAggregations(final List<AggregationBuilder<?>> aAggregationBuilders, QueryContext
    aQueryContext) {

    SearchRequestBuilder searchRequestBuilder = mClient.prepareSearch(mConfig.getIndex());

    BoolQueryBuilder globalAndFilter = QueryBuilders.boolQuery();

    if (!(null == aQueryContext)) {
      for (QueryBuilder contextFilter : aQueryContext.getFilters()) {
        globalAndFilter.must(contextFilter);
      }
    }

    for (AggregationBuilder<?> aggregationBuilder : aAggregationBuilders) {
      searchRequestBuilder.addAggregation(aggregationBuilder);
    }

    return searchRequestBuilder.setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), globalAndFilter))
      .setSize(0).execute().actionGet();

  }


  private ResourceList esQuery(@Nonnull final String aQueryString, final int aFrom, final int aSize,
                                 final String aSortOrder, final Map<String, List<String>> aFilters,
                                 final QueryContext aQueryContext, final boolean allowsTypos) {

    SearchRequestBuilder searchRequestBuilder = mClient.prepareSearch(mConfig.getIndex());
    BoolQueryBuilder globalAndFilter = QueryBuilders.boolQuery();

    String[] fieldBoosts = processQueryContext(aQueryContext, searchRequestBuilder, globalAndFilter);
    processSortOrder(aSortOrder, searchRequestBuilder);
    processFilters(aFilters, globalAndFilter);

    QueryBuilder queryBuilder = getQueryBuilder(aQueryString, fieldBoosts);
    FunctionScoreQueryBuilder fqBuilder = getFunctionScoreQueryBuilder(queryBuilder);
    final BoolQueryBuilder bqBuilder = QueryBuilders.boolQuery().filter(globalAndFilter);
    if (allowsTypos){
      bqBuilder.should(fqBuilder);
    }
    else {
      bqBuilder.must(fqBuilder);
    }
    searchRequestBuilder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setQuery(bqBuilder);

    List<SearchHit> searchHits = new ArrayList<>();
    SearchResponse response;
    Resource aAggregations;
    if (aSize == -1) {
      response = searchRequestBuilder.setScroll(new TimeValue(60000)).setSize(100).execute().actionGet();
      aAggregations = (Resource) Resource.fromJson(response.toString()).get("aggregations");
      List<SearchHit> nextHits = Arrays.asList(response.getHits().getHits());
      while (nextHits.size() > 0) {
        searchHits.addAll(nextHits);
        response = mClient.prepareSearchScroll(response.getScrollId()).setScroll(new TimeValue(60000)).execute()
          .actionGet();
        nextHits = Arrays.asList(response.getHits().getHits());
      }
    } else {
      response = searchRequestBuilder.setSize(aSize).execute().actionGet();
      aAggregations = (Resource) Resource.fromJson(response.toString()).get("aggregations");
      searchHits.addAll(Arrays.asList(response.getHits().getHits()));
    }

    List<Resource> resources = new ArrayList<>();
    for (SearchHit hit: searchHits) {
      Resource resource = Resource.fromMap(hit.sourceAsMap());
      resource.put("_score", hit.getScore());
      resources.add(resource);
    }

    return new ResourceList(resources, response.getHits().getTotalHits(), aQueryString, aFrom, aSize, aSortOrder,
      aFilters, aAggregations);

  }

  private FunctionScoreQueryBuilder getFunctionScoreQueryBuilder(QueryBuilder queryBuilder) {
    FunctionScoreQueryBuilder fqBuilder;
    fqBuilder = QueryBuilders.functionScoreQuery(queryBuilder);
    fqBuilder.boostMode(CombineFunction.MULT);
    fqBuilder.scoreMode(FiltersFunctionScoreQuery.ScoreMode.Sum.name().toLowerCase());
    fqBuilder.add(ScoreFunctionBuilders.fieldValueFactorFunction(Record.LINK_COUNT));
    return fqBuilder;
  }

  private QueryBuilder getQueryBuilder(@Nonnull String aQueryString, String[] fieldBoosts) {
    QueryBuilder queryBuilder;
    if (!StringUtils.isEmpty(aQueryString)) {
      if (aQueryString.endsWith("!")) {
        aQueryString = aQueryString.substring(0, aQueryString.lastIndexOf('!')).concat("\\!");
        Logger.trace("Modify query: insert escape '\\' in front of '!': ".concat(aQueryString));
      }
      queryBuilder = QueryBuilders.queryStringQuery(aQueryString).fuzziness(mFuzziness)
        .defaultOperator(QueryStringQueryBuilder.Operator.AND);
      if (fieldBoosts != null) {
        // TODO: extract fieldBoost parsing from loop in case
        for (String fieldBoost : fieldBoosts) {
          try {
            ((QueryStringQueryBuilder) queryBuilder).field(fieldBoost.split("\\^")[0],
              Float.parseFloat(fieldBoost.split("\\^")[1]));
          } catch (ArrayIndexOutOfBoundsException e) {
            Logger.trace("Invalid field boost: " + fieldBoost);
          }
        }
      }
    } else {
      queryBuilder = QueryBuilders.matchAllQuery();
    }
    return queryBuilder;
  }

  private void processFilters(Map<String, List<String>> aFilters, BoolQueryBuilder globalAndFilter) {
    if (!(null == aFilters)) {
      BoolQueryBuilder aggregationAndFilter = QueryBuilders.boolQuery();
      for (Map.Entry<String, List<String>> entry : aFilters.entrySet()) {
        BoolQueryBuilder orFilterBuilder = QueryBuilders.boolQuery();
        String filterName = entry.getKey();
        for (String filterValue : entry.getValue()) {
          if (filterName.endsWith(".GTE")) {
            filterName = filterName.substring(0, filterName.length()-".GTE".length());
            orFilterBuilder.should(QueryBuilders.rangeQuery(filterName).gte(filterValue));
          } else {
            // This could also be 'must' queries, allowing to narrow down the result list
            orFilterBuilder.should(QueryBuilders.termQuery(filterName, filterValue));
          }
        }
        aggregationAndFilter.must(orFilterBuilder);
      }
      globalAndFilter.must(aggregationAndFilter);
    }
  }

  private void processSortOrder(String aSortOrder, SearchRequestBuilder searchRequestBuilder) {
    if (!StringUtils.isEmpty(aSortOrder)) {
      String[] sort = aSortOrder.split(":");
      if (2 == sort.length) {
        searchRequestBuilder.addSort(new FieldSortBuilder(
          sort[0]).order(sort[1].toUpperCase().equals("ASC") ? SortOrder.ASC : SortOrder.DESC
        ).unmappedType("string"));
      } else {
        Logger.trace("Invalid sort string: " + aSortOrder);
      }

    }
  }

  private String[] processQueryContext(QueryContext aQueryContext, SearchRequestBuilder searchRequestBuilder, BoolQueryBuilder globalAndFilter) {
    String [] fieldBoosts = null;
    if (!(null == aQueryContext)) {
      searchRequestBuilder.setFetchSource(aQueryContext.getFetchSource(), null);
      for (QueryBuilder contextFilter : aQueryContext.getFilters()) {
        globalAndFilter.must(contextFilter);
      }
      for (AggregationBuilder<?> contextAggregation : aQueryContext.getAggregations()) {
        searchRequestBuilder.addAggregation(contextAggregation);
      }
      if (aQueryContext.hasFieldBoosts()) {
        fieldBoosts = aQueryContext.getElasticsearchFieldBoosts();
      }
      if (null != aQueryContext.getZoomTopLeft() && null != aQueryContext.getZoomBottomRight()) {
        GeoBoundingBoxQueryBuilder zoomFilter = QueryBuilders.geoBoundingBoxQuery("about.location.geo")
          .topLeft(aQueryContext.getZoomTopLeft())
          .bottomRight(aQueryContext.getZoomBottomRight());
        globalAndFilter.must(zoomFilter);
      }
      if (null != aQueryContext.getPolygonFilter() && !aQueryContext.getPolygonFilter().isEmpty()) {
        GeoPolygonQueryBuilder polygonFilter = QueryBuilders.geoPolygonQuery("about.location.geo");
        for (GeoPoint geoPoint : aQueryContext.getPolygonFilter()){
          polygonFilter.addPoint(geoPoint);
        }
        globalAndFilter.must(polygonFilter);
      }
    }
    return fieldBoosts;
  }

  public boolean hasIndex(String aIndex) {
    return mClient.admin().indices().prepareExists(aIndex).execute().actionGet().isExists();
  }

  private void refreshIndex(String aIndex) {
    try {
      mClient.admin().indices().refresh(new RefreshRequest(aIndex)).actionGet();
    } catch (IndexNotFoundException e) {
      Logger.error("Trying to refresh index \"" + aIndex + "\" in Elasticsearch.");
      e.printStackTrace();
    }
  }

  public void deleteIndex(String aIndex) {
    try {
      mClient.admin().indices().delete(new DeleteIndexRequest(aIndex)).actionGet();
    } catch (IndexNotFoundException e) {
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

  public ElasticsearchConfig getConfig() {
    return mConfig;
  }
}
