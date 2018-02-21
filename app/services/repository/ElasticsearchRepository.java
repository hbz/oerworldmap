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
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.lucene.search.function.FunctionScoreQuery;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.sort.SortOrder;
import play.Logger;
import services.ElasticsearchConfig;
import services.QueryContext;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;

public class ElasticsearchRepository extends Repository implements Readable, Writable, Queryable, Aggregatable {

  private static ElasticsearchConfig mConfig;
  private Fuzziness mFuzziness;
  private static JsonNodeFactory mJsonNodeFactory = new JsonNodeFactory(false);

  public ElasticsearchRepository(Config aConfiguration) {
    super(aConfiguration);
    mConfig = new ElasticsearchConfig(aConfiguration);

    final Settings.Builder builder = Settings.builder();
    mConfig.getClusterSettings().forEach((k, v) -> builder.put(k, v));
    Settings settings = builder.build();

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
  public Resource aggregate(@Nonnull AggregationBuilder aAggregationBuilder) throws IOException {
    return aggregate(aAggregationBuilder, null);
  }

  public Resource aggregate(@Nonnull AggregationBuilder aAggregationBuilder, QueryContext aQueryContext) {
    Resource aggregations = Resource
      .fromJson(getAggregation(aAggregationBuilder, aQueryContext).toString());
    if (null == aggregations) {
      return null;
    }
    return (Resource) aggregations.get("aggregations");
  }

  public Resource aggregate(@Nonnull List<AggregationBuilder> aAggregationBuilders, QueryContext aQueryContext) {
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

    SearchResponse response = esQuery(aQueryString, aFrom, aSize, aSortOrder, aFilters, aQueryContext, false);

    Iterator<SearchHit> searchHits = response.getHits().iterator();
    List<Resource> matches = new ArrayList<>();
    while (searchHits.hasNext()) {
      Resource match = Resource.fromMap(searchHits.next().getSourceAsMap());
      matches.add(match);
    }
    // FIXME: response.toString returns string serializations of scripted keys
    Resource aAggregations = (Resource) Resource.fromJson(response.toString()).get("aggregations");
    return new ResourceList(matches, response.getHits().getTotalHits(), aQueryString, aFrom, aSize, aSortOrder,
      aFilters, aAggregations);

  }

  public JsonNode reconcile(@Nonnull String aQuery, int aFrom, int aSize, String aSortOrder,
                            Map<String, List<String>> aFilters, QueryContext aQueryContext,
                            final Locale aPreferredLocale) {

    aQueryContext.setFetchSource(new String[]{"about.@id", "about.@type", "about.name"});

    SearchResponse response = esQuery(aQuery, aFrom, aSize, aSortOrder, aFilters, aQueryContext, true);
    Iterator<SearchHit> searchHits = response.getHits().iterator();
    ArrayNode resultItems = new ArrayNode(mJsonNodeFactory);

    while (searchHits.hasNext()) {
      final SearchHit hit = searchHits.next();
      Resource match = Resource.fromMap(hit.getSourceAsMap()).getAsResource(Record.RESOURCE_KEY);
      String name = match.getNestedFieldValue("name.@value", aPreferredLocale);
      ObjectNode item = new ObjectNode(mJsonNodeFactory);
      item.put("id", match.getId());
      item.put("match", aQuery.toLowerCase().replaceAll("[ ,\\.\\-_+]", "")
        .equals(name.toLowerCase().replaceAll("[ ,\\.\\-_+]", "")));
      item.put("name", name);
      item.put("score", hit.getScore());
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
    mConfig.getClient().prepareIndex(mConfig.getIndex(), aType, aUuid).setSource(aJsonString, XContentType.JSON).execute()
      .actionGet();
  }

  /**
   * Add documents consisting of JSON Strings specified by a given UUID and a
   * given type.
   *
   * @param aJsonStringIdMap
   */
  public void addJson(final Map<String, String> aJsonStringIdMap, final String aType) {

    BulkRequestBuilder bulkRequest = mConfig.getClient().prepareBulk();
    for (Map.Entry<String, String> entry : aJsonStringIdMap.entrySet()) {
      String id = entry.getKey();
      String json = entry.getValue();
      bulkRequest.add(mConfig.getClient().prepareIndex(mConfig.getIndex(), aType, id).setSource(json));
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
    while (response == null || response.getHits().getHits().length != 0) {
      response = mConfig.getClient().prepareSearch(mConfig.getIndex())
        .setQuery(QueryBuilders.queryStringQuery(aField.concat(":").concat(QueryParser.escape(aValue.toString()))))
        .setSize(docsPerPage).setFrom(count * docsPerPage).execute().actionGet();
      for (SearchHit hit : response.getHits()) {
        docs.add(hit.getSourceAsMap());
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
    final GetResponse response = mConfig.getClient().prepareGet(mConfig.getIndex(), aType, aIdentifier)
      .execute().actionGet();
    return response.getSource();
  }

  private boolean deleteDocument(@Nonnull final String aType, @Nonnull final String aIdentifier) {
    final DeleteResponse response = mConfig.getClient().prepareDelete(mConfig.getIndex(), aType, aIdentifier)
      .execute().actionGet();
    return response.status().equals(DocWriteResponse.Result.DELETED);
  }

  private SearchResponse getAggregation(final AggregationBuilder aAggregationBuilder, QueryContext aQueryContext) {

    SearchRequestBuilder searchRequestBuilder = mConfig.getClient().prepareSearch(mConfig.getIndex());

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

  private SearchResponse getAggregations(final List<AggregationBuilder> aAggregationBuilders, QueryContext
    aQueryContext) {

    SearchRequestBuilder searchRequestBuilder = mConfig.getClient().prepareSearch(mConfig.getIndex());

    BoolQueryBuilder globalAndFilter = QueryBuilders.boolQuery();

    if (!(null == aQueryContext)) {
      for (QueryBuilder contextFilter : aQueryContext.getFilters()) {
        globalAndFilter.must(contextFilter);
      }
    }

    for (AggregationBuilder aggregationBuilder : aAggregationBuilders) {
      searchRequestBuilder.addAggregation(aggregationBuilder);
    }

    return searchRequestBuilder.setQuery(globalAndFilter).setSize(0).execute().actionGet();

  }

  private SearchResponse esQuery(@Nonnull final String aQueryString, final int aFrom, final int aSize,
                                 final String aSortOrder, final Map<String, List<String>> aFilters,
                                 final QueryContext aQueryContext, final boolean allowsTypos) {

    SearchRequestBuilder searchRequestBuilder = mConfig.getClient().prepareSearch(mConfig.getIndex());
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
    else{
      bqBuilder.must(fqBuilder);
    }
    searchRequestBuilder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setQuery(bqBuilder);
    return searchRequestBuilder.setFrom(aFrom).setSize(aSize).execute().actionGet();
  }

  private FunctionScoreQueryBuilder getFunctionScoreQueryBuilder(QueryBuilder queryBuilder) {
    FunctionScoreQueryBuilder fqBuilder = QueryBuilders.functionScoreQuery(queryBuilder);
    fqBuilder.boostMode(CombineFunction.MULTIPLY);
    fqBuilder.scoreMode(FunctionScoreQuery.ScoreMode.SUM);
    // fqBuilder.add(ScoreFunctionBuilders.fieldValueFactorFunction(Record.LINK_COUNT));
    // TODO
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
        searchRequestBuilder.addSort(sort[0], sort[1].toUpperCase().equals("ASC") ? SortOrder.ASC : SortOrder.DESC);
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
      for (AggregationBuilder contextAggregation : aQueryContext.getAggregations()) {
        searchRequestBuilder.addAggregation(contextAggregation);
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
    return mConfig.getClient().admin().indices().prepareExists(aIndex).execute().actionGet().isExists();
  }

  private void refreshIndex(String aIndex) {
    try {
      mConfig.getClient().admin().indices().refresh(new RefreshRequest(aIndex)).actionGet();
    } catch (IndexNotFoundException e) {
      Logger.error("Trying to refresh index \"" + aIndex + "\" in Elasticsearch.");
      e.printStackTrace();
    }
  }

  public void deleteIndex(String aIndex) {
    try {
      mConfig.getClient().admin().indices().delete(new DeleteIndexRequest(aIndex)).actionGet();
    } catch (IndexNotFoundException e) {
      Logger.error("Trying to delete index \"" + aIndex + "\" from Elasticsearch.");
      e.printStackTrace();
    }
  }

  public void createIndex(String aIndex) {
    try {
      mConfig.getClient().admin().indices().prepareCreate(aIndex).setSource(mConfig.getIndexConfigString(), XContentType.JSON)
        .execute().actionGet();
      mConfig.getClient().admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet();
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
