package services;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;

import play.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
    URL url = new URL(mSearchStub + (StringUtils.isEmpty(aIndex) ? "_all" : (aIndex)) + "/"
        + (StringUtils.isEmpty(aType) ? "" : (aType + "/")) + "_search?size=99999&q=" + URLEncoder.encode(aEsQuery, "UTF-8"));
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setDoOutput(true);
    connection.connect();
    return searchResultToMaps(IOUtils.toString(connection.getInputStream(), "UTF-8"));
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> searchResultToMaps(String aEsSearchResultJson)
      throws JsonParseException, JsonMappingException, IOException, ParseException {

    List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();

    JSONParser jsonParser = new JSONParser();
    JSONObject jsonObject = (JSONObject) jsonParser.parse(aEsSearchResultJson);
    JSONObject hitsWrapper = (JSONObject) jsonObject.get("hits");
    JSONArray hits = (JSONArray) hitsWrapper.get("hits");

    ListIterator<JSONObject> iterator = hits.listIterator();
    while (iterator.hasNext()) {
      result.add((JSONObject) iterator.next().get("_source"));
    }

    return result;
  }
}
