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
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import play.Logger;
import services.ElasticsearchConfig;
import services.QueryContext;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;

import static org.elasticsearch.index.query.QueryBuilders.termQuery;

public class ElasticsearchRepository extends Repository implements Readable, Writable, Queryable, Aggregatable {

  private static ElasticsearchConfig mConfig;
  private Fuzziness mFuzziness;
  private static JsonNodeFactory mJsonNodeFactory = new JsonNodeFactory(false);

  public ElasticsearchRepository(Config aConfiguration) {
    super(aConfiguration);
    mConfig = new ElasticsearchConfig(aConfiguration);

    final Settings.Builder builder = Settings.builder();
    mConfig.getClusterSettings().forEach((k, v) -> builder.put(k, v));

    mFuzziness = mConfig.getFuzziness();
  }

  @Override
  public void addResource(@Nonnull final Resource aResource, Map<String, String> aMetadata) throws IOException {
    Record record = new Record(aResource);
    for (String key : aMetadata.keySet()) {
      record.put(key, aMetadata.get(key));
    }
    addJson(record.toString(), record.getId(), Record.TYPE);
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
    addJsonBulk(records, Record.TYPE);
  }

  @Override
  public Resource getResource(@Nonnull String aId) {
    try {
      return Resource.fromMap(getDocument(Record.TYPE, aId));
    } catch (IOException e) {
      Logger.error("Failed getting document.", e);
    }
    return null;
  }

  public List<Resource> getResources(@Nonnull String aField, @Nonnull Object aValue) {
    List<Resource> resources = new ArrayList<>();
    try {
      for (Map<String, Object> doc : getDocuments(aField, aValue)) {
        resources.add(Resource.fromMap(doc));
      }
    } catch (IOException e) {
      Logger.error("Failed getting multiple documents.", e);
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
    boolean found = false;
    try {
      found = deleteDocument(Record.TYPE, resource.getId());
    } catch (IOException e) {
      Logger.error("Failed deleting document.", e);
    }
    Logger.trace("Deleted " + aId + " from Elasticsearch");
    if (found) {
      return resource;
    } else {
      return null;
    }
  }

  @Override
  public Resource aggregate(@Nonnull AggregationBuilder aAggregationBuilder) throws IOException {
    return aggregate(aAggregationBuilder, null);
  }

  public Resource aggregate(@Nonnull AggregationBuilder aAggregationBuilder, QueryContext aQueryContext) {
    Resource aggregations = null;
    try {
      aggregations = Resource
        .fromJson(getAggregation(aAggregationBuilder, aQueryContext).toString());
    } catch (IOException e) {
      Logger.error("Failed getting aggregation.", e);
    }
    if (null == aggregations) {
      return null;
    }
    return (Resource) aggregations.get("aggregations");
  }

  public Resource aggregate(@Nonnull List<AggregationBuilder> aAggregationBuilders, QueryContext aQueryContext) {
    Resource aggregations = null;
    try {
      aggregations = Resource
        .fromJson(getAggregations(aAggregationBuilders, aQueryContext).toString());
    } catch (IOException e) {
      Logger.error("Failed getting multiple aggregations.", e);
    }
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

    return esQuery(aQueryString, aFrom, aSize, aSortOrder, aFilters, aQueryContext);

  }

  public JsonNode reconcile(@Nonnull String aQuery, int aFrom, int aSize, String aSortOrder,
                            Map<String, List<String>> aFilters, QueryContext aQueryContext,
                            final Locale aPreferredLocale) throws IOException {
    aQuery = QueryParser.escape(aQuery);
    aQuery = aQuery.replaceAll("([^ ]+)", "$1~");
    aQueryContext.setFetchSource(new String[]{"about.@id", "about.@type", "about.name"});

    ResourceList response = esQuery(aQuery, aFrom, aSize, aSortOrder, aFilters, aQueryContext);
    Iterator<Resource> searchHits = response.getItems().iterator();
    ArrayNode resultItems = new ArrayNode(mJsonNodeFactory);

    while (searchHits.hasNext()) {
      final Resource hit = searchHits.next();
      Resource match = hit.getAsResource(Record.RESOURCE_KEY);
      String name = match.getNestedFieldValue("name.@value", aPreferredLocale);
      ObjectNode item = new ObjectNode(mJsonNodeFactory);
      item.put("id", match.getId());
      item.put("match", !StringUtils.isEmpty(hit.getAsString("_score"))
        && Double.parseDouble(hit.getAsString("_score")) == 1.0);
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
    String uuid = getUrlUuidEncoded(aUuid);
    IndexRequest request = new IndexRequest(mConfig.getIndex(), aType, (uuid == null ? aUuid : uuid));
    request.source(aJsonString, XContentType.JSON);
    request.setRefreshPolicy(mConfig.getRefreshPolicy());
    // see https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-refresh.html,
    try {
      mConfig.getClient().index(request);
    } catch (IOException | ElasticsearchStatusException e) {
      Logger.error("Failed indexing data to Elasticsearch.", e);
    }
  }

  private String getUrlUuidEncoded (String aUuid) {
    if (isValidUri(aUuid)){
      try {
        return URLEncoder.encode(aUuid, Charset.defaultCharset().name());
      } catch (UnsupportedEncodingException e) {
        return null;
      }
    }
    else{
      return aUuid;
    }
  }

  private static boolean isValidUri (String aUri) {
    try {
      new URL(aUri);
    } catch (MalformedURLException mue) {
      return false;
    }
    return true;
  }

  /**
   * Add documents consisting of JSON Strings specified by a given UUID and a
   * given type.
   *
   * @param aJsonStringIdMap
   */
  public void addJsonBulk(final Map<String, String> aJsonStringIdMap, final String aType) {
    BulkRequest request = new BulkRequest();
    for (Map.Entry<String, String> entry : aJsonStringIdMap.entrySet()) {
      request.add(new IndexRequest(mConfig.getIndex(), aType, entry.getKey())
        .source(entry.getValue(), XContentType.JSON));
    }
    request.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
    BulkResponse bulkResponse = null;
    try {
      bulkResponse = mConfig.getClient().bulk(request);
    } catch (IOException e) {
      Logger.error("Failed indexing bulk data to Elasticsearch.", e);
    }
    if (bulkResponse.hasFailures()) {
      Logger.error(bulkResponse.buildFailureMessage());
    }
  }

  /**
   * get *all* matching documents as one list
   * @param aField
   * @param aValue
   * @return
   * @throws IOException
   */
  private List<Map<String, Object>> getDocuments(final String aField, final Object aValue) throws IOException {
    final int docsPerPage = 1024;
    int count = 0;
    SearchResponse response = null;
    final List<Map<String, Object>> docs = new ArrayList<>();

    SearchRequest searchRequest = new SearchRequest();
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    searchSourceBuilder
      .query(QueryBuilders.queryStringQuery(aField.concat(":").concat(QueryParser.escape(aValue.toString()))))
      .size(docsPerPage);
    searchRequest.source(searchSourceBuilder);
    while (response == null || response.getHits().getHits().length != 0) {
      searchSourceBuilder.from(count * docsPerPage);
      response = mConfig.getClient().search(searchRequest);
      for (SearchHit hit : response.getHits().getHits()) {
        docs.add(hit.getSourceAsMap());
      }
      count++;
    }
    return docs;
  }

  /**
   * Get a document of a specified type specified by an identifier.
   * @param aType
   * @param aIdentifier
   * @return the document as Map of String/Object
   */
  private Map<String, Object> getDocument(@Nonnull final String aType,
                                         @Nonnull final String aIdentifier) throws IOException {
    GetRequest request = new GetRequest(mConfig.getIndex(), aType, aIdentifier);
    // optionally: request.refresh(true);
    final GetResponse response = mConfig.getClient().get(request);
    return response.getSource();
  }

  private boolean deleteDocument(@Nonnull final String aType, @Nonnull final String aIdentifier) throws IOException {
    DeleteRequest request = new DeleteRequest(mConfig.getIndex(), aType, aIdentifier);
    request.setRefreshPolicy(mConfig.getRefreshPolicy());
    // see https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-refresh.html,
    final DeleteResponse response = mConfig.getClient().delete(request);
    return response.status().equals(RestStatus.OK);
  }


  private SearchResponse getAggregation(
    final AggregationBuilder aAggregationBuilder, final QueryContext aQueryContext) throws IOException {

    BoolQueryBuilder globalAndFilter = QueryBuilders.boolQuery();
    if (!(null == aQueryContext)) {
      for (QueryBuilder contextFilter : aQueryContext.getFilters()) {
        globalAndFilter.must(contextFilter);
      }
    }
    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
      .aggregation(aAggregationBuilder).query(globalAndFilter).size(0);

    return mConfig.getClient().search(
      new SearchRequest(mConfig.getIndex()).source(sourceBuilder));
  }


  private SearchResponse getAggregations(final List<AggregationBuilder> aAggregationBuilders, QueryContext
    aQueryContext) throws IOException {

    BoolQueryBuilder globalAndFilter = QueryBuilders.boolQuery();
    if (!(null == aQueryContext)) {
      for (QueryBuilder contextFilter : aQueryContext.getFilters()) {
        globalAndFilter.must(contextFilter);
      }
    }
    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
    for (AggregationBuilder aggregationBuilder : aAggregationBuilders) {
      sourceBuilder.aggregation(aggregationBuilder).query(globalAndFilter).size(0);
    }

    return mConfig.getClient().search(
      new SearchRequest(mConfig.getIndex()).source(sourceBuilder));
  }


  private ResourceList esQuery(@Nonnull final String aQueryString, final int aFrom, final int aSize,

                               final String aSortOrder, final Map<String, List<String>> aFilters,
                               final QueryContext aQueryContext) throws IOException {

    final SearchSourceBuilder sourceBuilder = new SearchSourceBuilder().from(aFrom);
    processSortOrder(aSortOrder, aQueryString, sourceBuilder);
    final BoolQueryBuilder globalAndFilter = QueryBuilders.boolQuery();
    processFilters(aFilters, globalAndFilter);
    final String[] fieldBoosts = processQueryContext(aQueryContext, sourceBuilder, globalAndFilter);

    QueryBuilder queryBuilder = getQueryBuilder(aQueryString, fieldBoosts);
    FunctionScoreQueryBuilder fqBuilder = getFunctionScoreQueryBuilder(queryBuilder);
    final BoolQueryBuilder bqBuilder = QueryBuilders.boolQuery().filter(globalAndFilter);
    bqBuilder.must(fqBuilder);
    sourceBuilder.query(bqBuilder);

    List<SearchHit> searchHits = new ArrayList<>();
    SearchResponse response;
    Resource aAggregations;
    Float maxScore = 0.0f;
    if (aSize == -1) {
      response = mConfig.getClient().search(
        new SearchRequest(mConfig.getIndex()).source(sourceBuilder).searchType(SearchType.DFS_QUERY_THEN_FETCH)
          .scroll(new TimeValue(60000)));
      maxScore = response.getHits().getMaxScore() > maxScore ? response.getHits().getMaxScore() : maxScore;
      aAggregations = (Resource) Resource.fromJson(response.toString()).get("aggregations");
      List<SearchHit> nextHits = Arrays.asList(response.getHits().getHits());
      while (nextHits.size() > 0) {
        searchHits.addAll(nextHits);
        SearchScrollRequest searchScrollRequest = new SearchScrollRequest()
          .scrollId(response.getScrollId()).scroll(new TimeValue(60000));
        response = mConfig.getClient().searchScroll(searchScrollRequest);
        nextHits = Arrays.asList(response.getHits().getHits());
        maxScore = response.getHits().getMaxScore() > maxScore ? response.getHits().getMaxScore() : maxScore;
      }
    } else {
      sourceBuilder.size(aSize);
      response = mConfig.getClient().search(new SearchRequest(mConfig.getIndex()).source(sourceBuilder));
      aAggregations = (Resource) Resource.fromJson(response.toString()).get("aggregations");
      searchHits.addAll(Arrays.asList(response.getHits().getHits()));
      maxScore = response.getHits().getMaxScore() > maxScore ? response.getHits().getMaxScore() : maxScore;
    }

    List<Resource> resources = new ArrayList<>();
    for (SearchHit hit: searchHits) {
      Resource resource = Resource.fromMap(hit.getSourceAsMap());
      if (!Float.isNaN(hit.getScore())) {
        // Convert ES scoring to score between 0 an 1
        resource.put("_score", hit.getScore() / maxScore);
      }
      resources.add(resource);
    }

    return new ResourceList(resources, response.getHits().getTotalHits(), aQueryString, aFrom, aSize, aSortOrder,
      aFilters, aAggregations);
  }


  private FunctionScoreQueryBuilder getFunctionScoreQueryBuilder(QueryBuilder queryBuilder) {

    ScoreFunctionBuilder fb = ScoreFunctionBuilders
      .scriptFunction("doc['".concat(Record.LINK_COUNT).concat("'].value < 1 ? _score : (_score * doc['")
        .concat(Record.LINK_COUNT).concat("'].value)"));
    FunctionScoreQueryBuilder fsb = new FunctionScoreQueryBuilder(queryBuilder, fb);
    return fsb;
  }

  private QueryBuilder getQueryBuilder(@Nonnull String aQueryString, String[] fieldBoosts) {
    QueryBuilder queryBuilder;
    if (!StringUtils.isEmpty(aQueryString)) {
      if (aQueryString.endsWith("!")) {
        aQueryString = aQueryString.substring(0, aQueryString.lastIndexOf('!')).concat("\\!");
        Logger.trace("Modify query: insert escape '\\' in front of '!': ".concat(aQueryString));
      }
      queryBuilder = QueryBuilders.queryStringQuery(aQueryString).fuzziness(mFuzziness)
        .defaultOperator(Operator.AND);
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
            orFilterBuilder.should(termQuery(filterName, filterValue));
          }
        }
        aggregationAndFilter.must(orFilterBuilder);
      }
      globalAndFilter.must(aggregationAndFilter);
    }
  }


  private void processSortOrder(String aSortOrder, String aQueryString, SearchSourceBuilder searchBuilder) {
    // Sort by dateCreated if no query string given
    if (StringUtils.isEmpty(aQueryString) && StringUtils.isEmpty(aSortOrder)) {
      aSortOrder = "dateCreated:DESC";
    }
    if (!StringUtils.isEmpty(aSortOrder)) {
      String[] sort = aSortOrder.split(":");
      if (2 == sort.length) {
        searchBuilder.sort(sort[0], sort[1].toUpperCase().equals("ASC") ? SortOrder.ASC : SortOrder.DESC);
      }
      else {
        Logger.trace("Invalid sort string: " + aSortOrder);
      }

    }
  }

  private String[] processQueryContext(
    QueryContext aQueryContext, SearchSourceBuilder sourceBuilder, BoolQueryBuilder globalAndFilter) {
    String [] fieldBoosts = null;
    if (!(null == aQueryContext)) {
      sourceBuilder.fetchSource(aQueryContext.getFetchSource(), null);
      for (QueryBuilder contextFilter : aQueryContext.getFilters()) {
        globalAndFilter.must(contextFilter);
      }
      for (AggregationBuilder contextAggregation : aQueryContext.getAggregations()) {
        sourceBuilder.aggregation(contextAggregation);
      }
      if (aQueryContext.hasFieldBoosts()) {
        fieldBoosts = aQueryContext.getElasticsearchFieldBoosts();
      }
      if (null != aQueryContext.getZoomTopLeft() && null != aQueryContext.getZoomBottomRight()) {
        GeoBoundingBoxQueryBuilder zoomFilter = QueryBuilders.geoBoundingBoxQuery("about.location.geo")
          .setCorners(aQueryContext.getZoomTopLeft(), aQueryContext.getZoomBottomRight());
        globalAndFilter.must(zoomFilter);
      }
      if (null != aQueryContext.getPolygonFilter() && !aQueryContext.getPolygonFilter().isEmpty()) {
        GeoPolygonQueryBuilder polygonFilter = QueryBuilders.geoPolygonQuery("about.location.geo",
          aQueryContext.getPolygonFilter());
        globalAndFilter.must(polygonFilter);
      }
    }
    return fieldBoosts;
  }

  public boolean hasIndex(String aIndex) {
    return mConfig.indexExists(aIndex);
  }

  public void deleteIndex(String aIndex) throws IOException {
    mConfig.deleteIndex(aIndex);
  }

  public void createIndex(String aIndex) throws IOException {
    mConfig.createIndex(aIndex);
  }

  public ElasticsearchConfig getConfig() {
    return mConfig;
  }
}
