package services.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import helpers.JsonLdConstants;
import helpers.Types;
import helpers.UniversalFunctions;
import models.*;
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
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.sort.SortOrder;
import play.Logger;
import services.ElasticsearchConfig;
import services.QueryContext;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ElasticsearchRepository extends Repository implements Readable, Writable, Queryable, Aggregatable {

  private static ElasticsearchConfig mConfig;
  private static Types mTypes;
  private Client mClient;
  private Fuzziness mFuzziness;
  private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public ElasticsearchRepository(final Config aConfiguration, final Types aTypes) throws IOException {
    super(aConfiguration);
    mTypes = aTypes;
    mConfig = new ElasticsearchConfig(aConfiguration, mTypes);
    Settings settings = Settings.settingsBuilder().put(mConfig.getClientSettings()).build();
    try {
      mClient = TransportClient.builder().settings(settings).build().addTransportAddress(
        new InetSocketTransportAddress(
          InetAddress.getByName(mConfig.getServer()), mConfig.getJavaPort()));
    } catch (UnknownHostException ex) {
      throw new RuntimeException(ex);
    }
    mFuzziness = mConfig.getFuzziness();
  }

  @Override
  public void addItem(@Nonnull final ModelCommon aItem, Map<String, Object> aMetadata) throws IOException {
    Record record = new Record(aItem, mTypes.getIndexType(aItem));
    for (String key : aMetadata.keySet()) {
      record.put(key, aMetadata.get(key));
    }
    addJson(record.toString(), record.getId(), aItem.getClass());
    refreshIndices(mConfig.getAllIndices());
  }

  @Override
  public void addItems(@Nonnull List<ModelCommon> aResources, Map<String, Object> aMetadata) throws IOException {
    Map<String, Record> records = new HashMap<>();
    for (ModelCommon resource : aResources) {
      Record record = new Record(resource, mTypes.getIndexType(resource));
      for (String key : aMetadata.keySet()) {
        record.put(key, aMetadata.get(key));
      }
      records.put(record.getId(), record);
    }
    addJson(records, Record.class.getName());
    refreshIndices(mConfig.getAllIndices());
  }

  @Override
  public Resource getItem(@Nonnull String aId) {
    return new Resource(getDocument(Record.class.getName(), aId));
  }

  public List<ModelCommon> getResources(@Nonnull String aField, @Nonnull Object aValue,
                                     final String... aIndices) {
    List<ModelCommon> resources = new ArrayList<>();
    for (Map<String, Object> doc : getDocuments(aField, aValue, aIndices)) {
      resources.add(new Resource(doc));
    }
    return resources;
  }

  @Override
  public List<ModelCommon> getAll(@Nonnull String aType, final String... aIndices) throws IOException {
    List<ModelCommon> resources = new ArrayList<>();
    for (Map<String, Object> doc : getDocuments(Record.CONTENT_KEY.concat(".")
      .concat(JsonLdConstants.TYPE), aType, aIndices)) {
      resources.add(new Resource(doc));
    }
    return resources;
  }

  @Override
  public ModelCommon deleteItem(@Nonnull final String aId,
                                @Nonnull final Class aClazz,
                                final Map<String, Object> aMetadata) {
    ModelCommon resource = getItem(aId.concat(".").concat(Record.CONTENT_KEY));
    if (null == resource) {
      return null;
    }
    boolean found = deleteDocument(aClazz, resource.getId());
    refreshIndices(mConfig.getAllIndices());
    Logger.trace("Deleted " + aId + " from Elasticsearch");
    if (found) {
      return resource;
    } else {
      return null;
    }
  }

  @Override
  public ModelCommon aggregate(@Nonnull AggregationBuilder<?> aAggregationBuilder, final String... aIndices)
    throws IOException {
    return aggregate(aAggregationBuilder, null, aIndices);
  }

  public ModelCommon aggregate(@Nonnull AggregationBuilder<?> aAggregationBuilder,
                            QueryContext aQueryContext, final String... aIndices) throws IOException {
    Resource aggregations =
      new Resource(OBJECT_MAPPER.readTree(
        getAggregation(aAggregationBuilder, aQueryContext, aIndices).toString()));
    if (null == aggregations) {
      return null;
    }
    return (Resource) aggregations.get("aggregations");
  }

  public ModelCommon aggregate(@Nonnull List<AggregationBuilder<?>> aAggregationBuilders,
                            QueryContext aQueryContext, final String... aIndices) throws IOException {
    Resource aggregations =
      new Resource(OBJECT_MAPPER.readTree(
        getAggregations(aAggregationBuilders, aQueryContext, aIndices).toString()));
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
   * @param aQueryString A string describing the query
   * @param aFilters
   * @return A resource resembling the result set of resources matching the
   * criteria given in the query string
   * @throws IOException
   */
  @Override
  public ModelCommonList query(@Nonnull String aQueryString, int aFrom, int aSize, String aSortOrder,
                               Map<String, List<String>> aFilters, final String... aIndices)
    throws IOException {
    return query(aQueryString, aFrom, aSize, aSortOrder, aFilters, null, aIndices);
  }

  public ResourceList query(@Nonnull String aQueryString, int aFrom, int aSize, String aSortOrder,
                            Map<String, List<String>> aFilters, QueryContext aQueryContext,
                            final String... aIndices) throws IOException {

    SearchResponse response = esQuery(aQueryString, aFrom, aSize, aSortOrder, aFilters,
      aQueryContext, aIndices);

    Iterator<SearchHit> searchHits = response.getHits().iterator();
    List<ModelCommon> matches = new ArrayList<>();
    while (searchHits.hasNext()) {
      Resource match = new Resource(searchHits.next().sourceAsMap());
      matches.add(match);
    }
    // FIXME: response.toString returns string serializations of scripted keys
    final Object aggregations = new Resource(OBJECT_MAPPER.readTree(response.toString())).get("aggregations");
    if (aggregations != null && aggregations instanceof Resource) {
      return new ResourceList(matches, response.getHits().getTotalHits(), aQueryString, aFrom, aSize, aSortOrder,
        aFilters, (Resource) aggregations);
    }
    return null;
  }

  /**
   * Add a document consisting of a JSON String specified by a given UUID and a
   * given type.
   *
   * @param aJsonString
   */
  public void addJson(final String aJsonString, final String aUuid, final Class aClass) {
    mClient.prepareIndex(mConfig.getIndex(Resource.class),
      mTypes.getIndexType(aClass), aUuid).setSource(aJsonString).execute().actionGet();
  }

  /**
   * Add documents consisting of JSON Strings specified by a given UUID and a
   * given type.
   *
   * @param aJsonStringIdMap
   */
  public void addJson(final Map<String, Record> aJsonStringIdMap, final String aType) {

    BulkRequestBuilder bulkRequest = mClient.prepareBulk();
    for (Map.Entry<String, Record> entry : aJsonStringIdMap.entrySet()) {
      String id = entry.getKey();
      Record json = entry.getValue();
      bulkRequest.add(mClient.prepareIndex(
        mConfig.getIndex(json.getRecordContent().getClass()), aType, id).setSource(json));
    }
    BulkResponse bulkResponse = bulkRequest.execute().actionGet();
    if (bulkResponse.hasFailures()) {
      Logger.error(bulkResponse.buildFailureMessage());
    }
  }


  private List<Map<String, Object>> getDocuments(final String aField, final Object aValue,
                                                 final String... aIndices) {
    final int docsPerPage = 1024;
    int count = 0;
    SearchResponse response = null;
    final List<Map<String, Object>> docs = new ArrayList<>();
    while (response == null || response.getHits().hits().length != 0) {
      response = mClient.prepareSearch(aIndices)
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
    final GetResponse response = mClient.prepareGet(mConfig.getIndex(Record.class), aType, aIdentifier)
      .execute().actionGet();
    return response.getSource();
  }

  private boolean deleteDocument(@Nonnull final Class aClass, @Nonnull final String aIdentifier) {
    DeleteResponse response = mClient.prepareDelete(
      mConfig.getIndex(aClass), mTypes.getIndexType(aClass), aIdentifier).execute().actionGet();
    return response.isFound();
  }


  private SearchResponse getAggregation(final AggregationBuilder<?> aAggregationBuilder,
                                        QueryContext aQueryContext, final String... aIndices) {

    SearchRequestBuilder searchRequestBuilder = mClient.prepareSearch(aIndices);

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

  private SearchResponse getAggregations(final List<AggregationBuilder<?>> aAggregationBuilders,
                                         QueryContext aQueryContext, final String... aIndices) {

    SearchRequestBuilder searchRequestBuilder = mClient.prepareSearch(aIndices);

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

  private SearchResponse esQuery(@Nonnull String aQueryString, int aFrom, int aSize,
                                 String aSortOrder, Map<String, List<String>> aFilters,
                                 QueryContext aQueryContext, final String... aIndices) {

    SearchRequestBuilder searchRequestBuilder = mClient.prepareSearch(aIndices);

    BoolQueryBuilder globalAndFilter = QueryBuilders.boolQuery();

    String[] fieldBoosts = null;

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
        for (GeoPoint geoPoint : aQueryContext.getPolygonFilter()) {
          polygonFilter.addPoint(geoPoint);
        }
        globalAndFilter.must(polygonFilter);
      }
    }

    if (!StringUtils.isEmpty(aSortOrder)) {
      String[] sort = aSortOrder.split(":");
      if (2 == sort.length) {
        searchRequestBuilder.addSort(sort[0], sort[1].toUpperCase().equals("ASC") ? SortOrder.ASC : SortOrder.DESC);
      } else {
        Logger.trace("Invalid sort string: " + aSortOrder);
      }
    }

    if (!(null == aFilters)) {
      BoolQueryBuilder aggregationAndFilter = QueryBuilders.boolQuery();
      for (Map.Entry<String, List<String>> entry : aFilters.entrySet()) {
        BoolQueryBuilder orFilterBuilder = QueryBuilders.boolQuery();
        String filterName = entry.getKey();
        for (String filterValue : entry.getValue()) {
          if (filterName.endsWith(".GTE")) {
            filterName = filterName.substring(0, filterName.length() - ".GTE".length());
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

    FunctionScoreQueryBuilder fqBuilder = QueryBuilders.functionScoreQuery(queryBuilder);
    fqBuilder.boostMode(CombineFunction.MULT);
    fqBuilder.scoreMode(FiltersFunctionScoreQuery.ScoreMode.Sum.name().toLowerCase());
    fqBuilder.add(ScoreFunctionBuilders.fieldValueFactorFunction(Record.LINK_COUNT));

    searchRequestBuilder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
      .setQuery(QueryBuilders.boolQuery().must(fqBuilder).filter(globalAndFilter));

    return searchRequestBuilder.setFrom(aFrom).setSize(aSize).execute().actionGet();
  }

  private void refreshIndices(@Nonnull String... aIndices) {
    for (String index : aIndices) {
      try {
        mClient.admin().indices().refresh(new RefreshRequest(index)).actionGet();
      } catch (IndexNotFoundException e) {
        Logger.error("Trying to refresh index \"" + index + "\" in Elasticsearch.");
        e.printStackTrace();
      }
    }
  }

  public boolean hasIndex(String aIndex) {
    return mClient.admin().indices().prepareExists(aIndex).execute().actionGet().isExists();
  }

  public void deleteIndex(String aIndex) {
    try {
      mClient.admin().indices().delete(new DeleteIndexRequest(aIndex)).actionGet();
    } catch (IndexNotFoundException e) {
      Logger.error("Trying to delete index \"" + aIndex + "\" from Elasticsearch.");
      e.printStackTrace();
    }
  }

  public ElasticsearchConfig getConfig() {
    return mConfig;
  }

  public Types getTypes(){
    return mTypes;
  }

  public void createIndex(final String aIndex, final String aIndexConfigFile) {
    try {
      final String config = UniversalFunctions.readFile(aIndexConfigFile, StandardCharsets.UTF_8);
      mClient.admin().indices().prepareCreate(aIndex).setSource(config).execute().actionGet();
      mClient.admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet();
    } catch (ElasticsearchException indexAlreadyExists) {
      Logger.error("Trying to create index \"" + aIndex + "\" in Elasticsearch. Index already exists.");
      indexAlreadyExists.printStackTrace();
    } catch (IOException ioException) {
      Logger.error("Trying to create index \"" + aIndex + "\" in Elasticsearch. Couldn't read index config file.");
      ioException.printStackTrace();
    }
  }
}
