package services;

import java.io.IOException;
import java.util.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import controllers.Global;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.*;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.json.simple.parser.ParseException;

import play.Logger;

public class ElasticsearchProvider {

  private static ElasticsearchConfig mConfig;

  private Client mClient;

  public static String user;

  public static boolean excludeConcepts = true;

  private static FilterBuilder excludeFilter = FilterBuilders.notFilter(
      FilterBuilders.orFilter(
          FilterBuilders.termFilter("about.@type", "Concept"),
          FilterBuilders.termFilter("about.@type", "ConceptScheme")
      )
  );

  /**
   * Initialize an instance with a specified non null Elasticsearch client.
   *
   * @param aClient
   * @param aConfig
   */
  public ElasticsearchProvider(@Nullable final Client aClient, ElasticsearchConfig aConfig) {
    mClient = aClient;
    mConfig = aConfig;
  }

  public Client getClient() {
    return mClient;
  }

  public static String getIndex() {
    return mConfig.getIndex();
  }

  public static String getType() {
    return mConfig.getType();
  }

  public static void createIndex(Client aClient, String aIndex) {
    if (aClient == null) {
      throw new java.lang.IllegalStateException(
          "Trying to set Elasticsearch index with no existing client.");
    }
    if (indexExists(aClient, aIndex)) {
      Logger.warn("Index " + aIndex + " already exists while trying to create it.");
    }
    else{
      aClient.admin().indices().prepareCreate(aIndex).execute().actionGet();
    }
  }

  // return true if the specified Index already exists on the specified Client.
  public static boolean indexExists(Client aClient, String aIndex) {
    return aClient.admin().indices().prepareExists(aIndex).execute().actionGet().isExists();
  }

  /**
   * Add a document consisting of a JSON String specified by a given UUID.
   *
   * @param aJsonString
   */
  public void addJson(@Nonnull final String aJsonString, @Nullable final UUID aUuid) {
    if (aUuid == null) {
      mClient.prepareIndex(mConfig.getIndex(), mConfig.getType(), null).setSource(aJsonString)
          .execute().actionGet();
    } else {
      mClient.prepareIndex(mConfig.getIndex(), mConfig.getType(), aUuid.toString())
          .setSource(aJsonString).execute().actionGet();
    }
  }

  /**
   * Add a document consisting of a JSON String specified by a given UUID.
   *
   * @param aJsonString
   */
  public void addJson(@Nonnull final String aJsonString, @Nullable final String aUuid) {
    mClient.prepareIndex(mConfig.getIndex(), mConfig.getType(), aUuid).setSource(aJsonString)
        .execute().actionGet();
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
   * Add a document consisting of a Map specified by a given UUID.
   *
   * @param aMap
   */
  public void addMap(final Map<String, Object> aMap, final UUID aUuid) {
    mClient.prepareIndex(mConfig.getIndex(), mConfig.getType(), aUuid.toString())
        .setSource(aMap).execute().actionGet();
  }

  /**
   * Get all documents of a given document type
   *
   * @param aType
   * @return a List of docs, each represented by a Map of String/Object.
   */
  public List<Map<String, Object>> getAllDocs(final String aType) {
    final int docsPerPage = 1024;
    int count = 0;
    SearchResponse response = null;
    final List<Map<String, Object>> docs = new ArrayList<Map<String, Object>>();
    while (response == null || response.getHits().hits().length != 0) {
      response = mClient.prepareSearch(mConfig.getIndex()).setTypes(aType)
          .setQuery(QueryBuilders.matchAllQuery()).setSize(docsPerPage)
          .setFrom(count * docsPerPage).execute().actionGet();
      for (SearchHit hit : response.getHits()) {
        docs.add(hit.getSource());
      }
      count++;
    }
    return docs;
  }

  /**
   * Get an aggregation of documents
   *
   * @param aAggregationBuilder
   * @return a List of docs, each represented by a Map of String/Object.
   */
  public SearchResponse getAggregation(final AggregationBuilder<?> aAggregationBuilder) {
    SearchRequestBuilder searchRequestBuilder = mClient.prepareSearch(mConfig.getIndex());
    SearchResponse response = searchRequestBuilder.addAggregation(aAggregationBuilder)
        .setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), excludeFilter))
        .setSize(0).execute().actionGet();
    return response;
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

  /**
   * Get a document of a specified type specified by a UUID.
   *
   * @param aType
   * @param aUuid
   * @return the document as Map of String/Object
   */
  public Map<String, Object> getDocument(@Nonnull final String aType, @Nonnull final UUID aUuid) {
    return getDocument(aType, aUuid.toString());
  }

  public boolean deleteDocument(@Nonnull final String aType, @Nonnull final String aIdentifier) {
    final DeleteResponse response = mClient.prepareDelete(mConfig.getIndex(), aType, aIdentifier)
        .execute().actionGet();
    return response.isFound();
  }

  /**
   * Verify if the specified index exists on the internal Elasticsearch client.
   *
   * @param aIndex
   * @return true if the specified index exists.
   */
  public boolean hasIndex(String aIndex) {
    return mClient.admin().indices().prepareExists(aIndex).execute().actionGet().isExists();
  }

  /**
   * Deletes the specified index. If the specified index does not exist, the
   * resulting IndexMissingException is caught.
   *
   * @param aIndex
   */
  public void deleteIndex(String aIndex) {
    try {
      mClient.admin().indices().delete(new DeleteIndexRequest(aIndex)).actionGet();
    } catch (IndexMissingException e) {
      Logger.error("Trying to delete index \"" + aIndex + "\" from Elasticsearch.");
      e.printStackTrace();
    }
  }

  /**
   * Creates the specified index. If the specified index does already exist, the
   * resulting ElasticsearchException is caught.
   *
   * @param aIndex
   */
  public void createIndex(String aIndex) {
    try {
      mClient.admin().indices().prepareCreate(aIndex).setSource(mConfig.getIndexConfigString())
          .execute().actionGet();
      mClient.admin().cluster().prepareHealth()
          .setWaitForYellowStatus().execute().actionGet();
    } catch (ElasticsearchException indexAlreadyExists) {
      Logger.error("Trying to create index \"" + aIndex
          + "\" in Elasticsearch. Index already exists.");
      indexAlreadyExists.printStackTrace();
    } catch (IOException ioException) {
      Logger.error("Trying to create index \"" + aIndex
          + "\" in Elasticsearch. Couldn't read index config file.");
      ioException.printStackTrace();
    }
  }

  /**
   * Refreshes the specified index. If the specified index does not exist, the
   * resulting IndexMissingException is caught.
   *
   * @param aIndex
   */
  public void refreshIndex(String aIndex) {
    try {
      mClient.admin().indices().refresh(new RefreshRequest(aIndex)).actionGet();
    } catch (IndexMissingException e) {
      Logger.error("Trying to refresh index \"" + aIndex + "\" in Elasticsearch.");
      e.printStackTrace();
    }
  }

  public SearchResponse esQuery(@Nonnull String aEsQuery) throws IOException,
      ParseException {
    return esQuery(aEsQuery, null, null);
  }

  public SearchResponse esQuery(@Nonnull String aEsQuery, @Nullable String aIndex,
      @Nullable String sort) throws IOException, ParseException {
    return esQuery(aEsQuery, aIndex, null, sort);
  }

  // TODO: make this the only available method signature?
  public SearchResponse esQuery(@Nonnull String aQueryString, int aFrom, int aSize, String aSortOrder,
                                Map<String, ArrayList<String>> aFilters) {

    SearchRequestBuilder searchRequestBuilder = mClient
        .prepareSearch(mConfig.getIndex());

    // TODO: define query fields somewhere else
    searchRequestBuilder.setFetchSource(new String[]{
      "about.@id", "about.@type", "about.name", "about.location", "about.image",
      "about.provider.@id", "about.provider.@type", "about.provider.name", "about.provider.location",
      "about.participant.@id", "about.participant.@type", "about.participant.name", "about.participant.location",
      "about.agent.@id", "about.agent.@type", "about.agent.name", "about.agent.location",
      "about.mentions.@id", "about.mentions.@type", "about.mentions.name", "about.mentions.location",
      "about.mainEntity.@id", "about.mainEntity.@type", "about.mainEntity.name", "about.mainEntity.location"
    }, null);

    if (!StringUtils.isEmpty(aSortOrder)) {
      String[] sort = aSortOrder.split(":");
      if (2 == sort.length) {
        searchRequestBuilder.addSort(sort[0], sort[1].toUpperCase().equals("ASC") ? SortOrder.ASC
            : SortOrder.DESC);
      } else {
        Logger.error("Invalid sort string: " + aSortOrder);
      }
    }

    AndFilterBuilder globalAndFilter = FilterBuilders.andFilter();

    if (!(null == aFilters)) {
      AndFilterBuilder aggregationAndFilter = FilterBuilders.andFilter();
      for (Map.Entry<String, ArrayList<String>> entry : aFilters.entrySet()) {
        // This could also be an OrFilterBuilder allowing to expand the result list
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
      queryBuilder = QueryBuilders.queryString(aQueryString).defaultOperator(QueryStringQueryBuilder.Operator.AND);
    } else {
      queryBuilder = QueryBuilders.matchAllQuery();
    }

    // TODO: where should aggregations be added?
    searchRequestBuilder.addAggregation(AggregationProvider.getTypeAggregation());
    searchRequestBuilder.addAggregation(AggregationProvider.getByCountryAggregation());
    searchRequestBuilder.addAggregation(AggregationProvider.getServiceLanguageAggregation());
    searchRequestBuilder.addAggregation(AggregationProvider.getServiceByGradeLevelAggregation());

    // TODO: where should these filters be added?
    if (excludeConcepts) {
      globalAndFilter.add(excludeFilter);
    } else {
      excludeConcepts = true;
    }

    // TODO: Remove privacy filter when all persons are accounts?
    if (!Global.getConfig().getString("admin.user").equals(user)) {
      globalAndFilter.add(FilterBuilders.notFilter(FilterBuilders.andFilter(FilterBuilders
          .termFilter("about.@type", "Person"), FilterBuilders.notFilter(FilterBuilders.existsFilter("about.email")))));
    }

    searchRequestBuilder.setQuery(QueryBuilders.filteredQuery(queryBuilder, globalAndFilter));

    return searchRequestBuilder.setFrom(aFrom).setSize(aSize).execute().actionGet();

  }

  public SearchResponse esQuery(@Nonnull String aEsQuery, @Nullable String aIndex,
      @Nullable String aType, @Nullable String aSort) throws IOException, ParseException {
    SearchRequestBuilder searchRequestBuilder = mClient
        .prepareSearch(StringUtils.isEmpty(aIndex) ? mConfig.getIndex() : aIndex);
    if (!StringUtils.isEmpty(aType)) {
      searchRequestBuilder.setTypes(aType);
    }
    if (!StringUtils.isEmpty(aSort)) {
      String[] sort = aSort.split(":");
      if (2 == sort.length) {
        searchRequestBuilder.addSort(sort[0], sort[1].toUpperCase().equals("ASC") ? SortOrder.ASC
            : SortOrder.DESC);
      } else {
        Logger.error("Invalid sort string: " + aSort);
      }
    }

    return searchRequestBuilder
        .setQuery(
            QueryBuilders.queryString(aEsQuery).defaultOperator(
                QueryStringQueryBuilder.Operator.AND)).setFrom(0).setSize(1).execute()
        .actionGet();

  }
}
