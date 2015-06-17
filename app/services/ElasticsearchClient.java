package services;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.json.simple.parser.ParseException;
import play.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * This class serves as an interface to Elasticsearch providing access to
 * necessary functions in easy mode. It also serves to set up an Elasticsearch
 * instance appropriate to the settings specified in the configuration file
 * (most probably conf/application.conf).
 * 
 * @author pvb
 *
 */
public class ElasticsearchClient {

  private static ElasticsearchConfig mEsConfig;
  private static Client mClient;
  private static String mSearchStub;

  /**
   * Initialize an instance with a specified non null Elasticsearch client.
   * 
   * @param aClient
   * @param aEsConfig
   */
  public ElasticsearchClient(@Nullable final Client aClient, ElasticsearchConfig aEsConfig) {
    mClient = aClient;
    mEsConfig = aEsConfig;
    mSearchStub = "http://" + mEsConfig.getServer() + ":" + mEsConfig.getHttpPort() + "/";
  }

  public static Client getClient() {
    if (mClient == null) {
      mClient = ElasticsearchProvider.getClient(ElasticsearchProvider.createServerNode(true));
    }
    return mClient;
  }

  public static String getIndex() {
    return mEsConfig.getIndex();
  }

  public static String getType() {
    return mEsConfig.getType();
  }

  /**
   * Add a document consisting of a JSON String.
   * 
   * @param aJsonString
   */
  public void addJson(final String aJsonString) {
    addJson(aJsonString, UUID.randomUUID());
  }

  /**
   * Add a document consisting of a JSON String specified by a given UUID.
   * 
   * @param aJsonString
   */
  public void addJson(@Nonnull final String aJsonString, @Nullable final UUID aUuid) {
    if (aUuid == null) {
      mClient.prepareIndex(mEsConfig.getIndex(), mEsConfig.getType(), null).setSource(aJsonString)
          .execute().actionGet();
    } else {
      mClient.prepareIndex(mEsConfig.getIndex(), mEsConfig.getType(), aUuid.toString())
          .setSource(aJsonString).execute().actionGet();
    }
  }

  /**
   * Add a document consisting of a JSON String specified by a given UUID.
   * 
   * @param aJsonString
   */
  public void addJson(@Nonnull final String aJsonString, @Nullable final String aUuid) {
    mClient.prepareIndex(mEsConfig.getIndex(), mEsConfig.getType(), aUuid).setSource(aJsonString)
        .execute().actionGet();
  }

  /**
   * Add a document consisting of a JSON String specified by a given UUID and a
   * given type.
   *
   * @param aJsonString
   */
  public void addJson(final String aJsonString, final String aUuid, final String aType) {
    mClient.prepareIndex(mEsConfig.getIndex(), aType, aUuid).setSource(aJsonString).execute()
        .actionGet();
  }

  /**
   * Add a document consisting of a Map.
   * 
   * @param aMap
   */
  public void addMap(final Map<String, Object> aMap) {
    addMap(aMap, UUID.randomUUID());
  }

  /**
   * Add a document consisting of a Map specified by a given UUID.
   * 
   * @param aMap
   */
  public void addMap(final Map<String, Object> aMap, final UUID aUuid) {
    mClient.prepareIndex(mEsConfig.getIndex(), mEsConfig.getType(), aUuid.toString())
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
      response = mClient.prepareSearch(mEsConfig.getIndex()).setTypes(aType)
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
  public ArrayList<Object> getAggregation(final AggregationBuilder aAggregationBuilder) {
    final ArrayList<Object> entries = new ArrayList<Object>();

    SearchResponse response = mClient.prepareSearch(mEsConfig.getIndex())
        .addAggregation(aAggregationBuilder).setSize(0).execute().actionGet();
    Aggregation aggregation = response.getAggregations().asList().get(0);
    for (Terms.Bucket entry : ((Terms) aggregation).getBuckets()) {
      Map<String, Object> e = new HashMap<>();
      e.put("key", entry.getKey());
      e.put("value", entry.getDocCount());
      entries.add(e);
    }
    return entries;
  }

  /**
   * Get an aggregation of documents
   *
   * @param aAggregationBuilders
   * @return a List of docs, each represented by a Map of String/Object.
   */
  public Map<String, Object> getAggregations(final List<AggregationBuilder> aAggregationBuilders) {

    final Map<String, Object> result = new HashMap<>();
    final ArrayList<String> dimensions = new ArrayList<>();
    final ArrayList<Map> entries = new ArrayList<>();


    SearchRequestBuilder searchRequestBuilder = mClient.prepareSearch(mEsConfig.getIndex());
    for (AggregationBuilder aggregationBuilder : aAggregationBuilders) {
      searchRequestBuilder.addAggregation(aggregationBuilder);
    }

    SearchResponse response = searchRequestBuilder.setSize(0).execute().actionGet();

    List<Aggregation> aggregations = response.getAggregations().asList();

    final Map<String, ArrayList<Map>> observations = new HashMap<>();

    for (Aggregation aggregation : aggregations) {
      dimensions.add(aggregation.getName());
      for (Terms.Bucket entry : ((Terms) aggregation).getBuckets()) {
        String key = entry.getKey();
        long value = entry.getDocCount();
        if (null == observations.get(key)) {
          observations.put(key, new ArrayList<>());
        }
        Map<String, Object> observation = new HashMap<>();
        observation.put("dimension", aggregation.getName());
        observation.put("value", value);
        observations.get(key).add(observation);
      }
    }

    for (Map.Entry<String, ArrayList<Map>> observation : observations.entrySet()) {
      Map<String, Object> entry = new HashMap<>();
      entry.put("key", observation.getKey());

      List<Map> obs = observation.getValue();
      for (int i = 0; i < obs.size(); i++) {
        if (!dimensions.get(i).equals(obs.get(i).get("dimension"))) {
          Map<String, Object> ob = new HashMap<>();
          ob.put("dimension", dimensions.get(i));
          ob.put("value", 0);
          obs.add(i, ob);
        }
      }
      entry.put("observations", obs);
      entries.add(entry);
    }

    result.put("dimensions", dimensions);
    result.put("entries", entries);

    return result;
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
    final GetResponse response = mClient.prepareGet(mEsConfig.getIndex(), aType, aIdentifier)
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

  /**
   * Get a document of a specified type specified by a content in a specified
   * field.
   * 
   * @param aType
   * @param aFieldName
   * @param aContent
   * @return
   */
  public List<Map<String, Object>> getExactMatches(@Nonnull final String aType,
      @Nonnull final String aFieldName, @Nonnull final String aContent) {

    if (!hasIndex(mEsConfig.getIndex())) {
      createIndex(mEsConfig.getIndex());
    }
    List<Map<String, Object>> matches = new LinkedList<Map<String, Object>>();

    final SearchResponse response = mClient.prepareSearch(mEsConfig.getIndex()).setTypes(aType)
        .setQuery(QueryBuilders.matchQuery(aFieldName, aContent)).execute().actionGet();

    Iterator<SearchHit> searchHits = response.getHits().iterator();
    while (searchHits.hasNext()) {
      matches.add(searchHits.next().sourceAsMap());
    }
    return matches;
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
      mClient.admin().indices().prepareCreate(aIndex).setSource(mEsConfig.getIndexConfigString())
          .execute().actionGet();
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

  public List<Map<String, Object>> esQuery(@Nonnull String aEsQuery) throws IOException,
      ParseException {
    return esQuery(aEsQuery, null);
  }

  public List<Map<String, Object>> esQuery(@Nonnull String aEsQuery, @Nullable String aIndex)
      throws IOException, ParseException {
    return esQuery(aEsQuery, aIndex, null);
  }

  public List<Map<String, Object>> esQuery(@Nonnull String aEsQuery, @Nullable String aIndex,
      @Nullable String aType) throws IOException, ParseException {
    SearchRequestBuilder searchRequestBuilder = mClient.prepareSearch(
        StringUtils.isEmpty(aIndex) ? mEsConfig.getIndex() : aIndex);
    if (!StringUtils.isEmpty(aType)) {
      searchRequestBuilder.setTypes(aType);
    }
    SearchResponse response = searchRequestBuilder
        .setQuery(QueryBuilders.queryString(aEsQuery).defaultOperator(QueryStringQueryBuilder.Operator.AND))
        .setFrom(0).setSize(99999)
        .execute().actionGet();
    Iterator<SearchHit> searchHits = response.getHits().iterator();
    List<Map<String, Object>> matches = new ArrayList<>();
    while (searchHits.hasNext()) {
      matches.add(searchHits.next().sourceAsMap());
    }
    return matches;
  }

}
