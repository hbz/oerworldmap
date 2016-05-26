package services.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

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
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
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
import models.TripleCommit;
import play.Logger;
import services.ElasticsearchConfig;
import services.QueryContext;

public class ElasticsearchRepository extends Repository implements Readable, Writable, Queryable, Aggregatable {

  private static ElasticsearchConfig mConfig;
  private Client mClient;

  //final private ElasticsearchProvider elasticsearch;

  @SuppressWarnings("resource")
  public ElasticsearchRepository(Config aConfiguration) {
    super(aConfiguration);
    mConfig = new ElasticsearchConfig(aConfiguration);
    Settings settings = ImmutableSettings.settingsBuilder().put(mConfig.getClientSettings()).build();
    mClient = new TransportClient(settings)
      .addTransportAddress(new InetSocketTransportAddress(mConfig.getServer(), 9300));
  }

  @Override
  public void addResource(@Nonnull final Resource aResource, Map<String, String> aMetadata) throws IOException {
    Record record = new Record(aResource);
    record.put(Record.DATE_CREATED, aMetadata.get(Record.DATE_CREATED));
    record.put(Record.DATE_MODIFIED, aMetadata.get(TripleCommit.Header.DATE_HEADER));
    record.put(Record.AUTHOR, aMetadata.get(TripleCommit.Header.AUTHOR_HEADER));
    addJson(record.toString(), record.getId(), Record.TYPE);
    refreshIndex(mConfig.getIndex());
  }

  @Override
  public void addResources(@Nonnull List<Resource> aResources, Map<String, String> aMetadata) throws IOException {
    Map<String, String> records = new HashMap<>();
    for (Resource resource : aResources) {
      Record record = new Record(resource);
      record.put(Record.DATE_CREATED, aMetadata.get(Record.DATE_CREATED));
      record.put(Record.DATE_MODIFIED, aMetadata.get(TripleCommit.Header.DATE_HEADER));
      record.put(Record.AUTHOR, aMetadata.get(TripleCommit.Header.AUTHOR_HEADER));
      records.put(record.getId(), record.toString());
    }
    addJson(records, Record.TYPE);
    refreshIndex(mConfig.getIndex());
  }

  @Override
  public Resource getResource(@Nonnull String aId) {
    Map<String, Object> resourceMap = getDocument(Record.TYPE, aId);
    if (resourceMap != null) {
      return unwrapRecord(Resource.fromMap(resourceMap));
    }
    return null;
  }

  public Resource getRecord(@Nonnull String aId) {
    return Resource.fromMap(getDocument(Record.TYPE, aId));
  }

  public List<Resource> getResources(@Nonnull String aField, @Nonnull Object aValue) {
    List<Resource> resources = new ArrayList<>();
    for (Map<String, Object> doc : getDocuments(aField, aValue)) {
      resources.add(Resource.fromMap(doc));
    }
    return unwrapRecords(resources);
  }

  @Override
  public List<Resource> getAll(@Nonnull String aType) throws IOException {
    List<Resource> resources = new ArrayList<>();
    for (Map<String, Object> doc : getDocuments(Record.RESOURCE_KEY.concat(".")
      .concat(JsonLdConstants.TYPE), aType)) {
      resources.add(Resource.fromMap(doc));
    }
    return unwrapRecords(resources);
  }

  @Override
  public Resource deleteResource(@Nonnull String aId, Map<String, String> aMetadata) {
    Resource resource = getResource(aId.concat(".").concat(Record.RESOURCE_KEY));
    if (null == resource) {
      return null;
    }
    Logger.debug("DELETING " + aId);
    boolean found = deleteDocument(Record.TYPE, resource.getId().concat(".").concat(Record.RESOURCE_KEY));
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
    Resource aggregations = Resource
      .fromJson(getAggregation(aAggregationBuilder, aQueryContext).toString());
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
      Resource match = unwrapRecord(Resource.fromMap(searchHits.next().sourceAsMap()));
      matches.add(match);
    }
    // FIXME: response.toString returns string serializations of scripted keys
    Resource aAggregations = (Resource) Resource.fromJson(response.toString()).get("aggregations");
    return new ResourceList(matches, response.getHits().getTotalHits(), aQueryString, aFrom, aSize, aSortOrder,
      aFilters, aAggregations);

  }

  private List<Resource> unwrapRecords(List<Resource> aRecords) {
    List<Resource> resources = new ArrayList<>();
    for (Resource rec : aRecords) {
      resources.add(unwrapRecord(rec));
    }
    return resources;
  }

  private Resource unwrapRecord(Resource aRecord) {
    return aRecord.getAsResource(Record.RESOURCE_KEY);
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

  public List<Map<String, Object>> getDocuments(final String aField, final Object aValue) {
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
    return docs;
  }

  /**
   * Get a document of a specified type specified by an identifier.
   *
   * @param aType
   * @param aIdentifier
   * @return the document as Map of String/Object
   */
  public Map<String, Object> getDocument(@Nonnull final String aType,
                                         @Nonnull final String aIdentifier) {
    final GetResponse response = mClient.prepareGet(mConfig.getIndex(), aType, aIdentifier)
      .execute().actionGet();
    return response.getSource();
  }

  public boolean deleteDocument(@Nonnull final String aType, @Nonnull final String aIdentifier) {
    final DeleteResponse response = mClient.prepareDelete(mConfig.getIndex(), aType, aIdentifier)
      .execute().actionGet();
    return response.isFound();
  }

  public SearchResponse getAggregation(final AggregationBuilder<?> aAggregationBuilder, QueryContext aQueryContext) {

    SearchRequestBuilder searchRequestBuilder = mClient.prepareSearch(mConfig.getIndex());

    AndFilterBuilder globalAndFilter = FilterBuilders.andFilter();
    if (!(null == aQueryContext)) {
      for (FilterBuilder contextFilter : aQueryContext.getFilters()) {
        globalAndFilter.add(contextFilter);
      }
    }

    SearchResponse response = searchRequestBuilder.addAggregation(aAggregationBuilder)
      .setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), globalAndFilter))
      .setSize(0).execute().actionGet();
    return response;

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

  public boolean hasIndex(String aIndex) {
    return mClient.admin().indices().prepareExists(aIndex).execute().actionGet().isExists();
  }

  public void refreshIndex(String aIndex) {
    try {
      mClient.admin().indices().refresh(new RefreshRequest(aIndex)).actionGet();
    } catch (IndexMissingException e) {
      Logger.error("Trying to refresh index \"" + aIndex + "\" in Elasticsearch.");
      e.printStackTrace();
    }
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

}
