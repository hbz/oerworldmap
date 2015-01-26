package services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

public class ElasticsearchClient {

  private static ElasticsearchConfig esConfig = new ElasticsearchConfig();
  private static Client mClient;

  public ElasticsearchClient(@Nonnull final Client aClient) {
    mClient = aClient;
  }

  public static Client getClient() {
    if (mClient == null){
      mClient = ElasticsearchProvider.getClient(ElasticsearchProvider.createServerNode(true));
    }
    return mClient;
  }

  public static String getIndex() {
    return esConfig.getIndex();
  }

  public static String getType() {
    return esConfig.getType();
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
  public void addJson(final String aJsonString, final UUID aUuid) {
    mClient.prepareIndex(esConfig.getIndex(), esConfig.getType(), aUuid.toString()).setSource(aJsonString).execute()
        .actionGet();
  }
  
  /**
   * Add a document consisting of a JSON String specified by a given UUID.
   * 
   * @param aJsonString
   */
  public void addJson(final String aJsonString, final String aUuid) {
    mClient.prepareIndex(esConfig.getIndex(), esConfig.getType(), aUuid).setSource(aJsonString).execute()
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
    mClient.prepareIndex(esConfig.getIndex(), esConfig.getType(), aUuid.toString()).setSource(aMap).execute().actionGet();
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
      response = mClient.prepareSearch(esConfig.getIndex()).setTypes(aType)
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
   * Get a document of a specified type specified by an identifier.
   * 
   * @param aType
   * @param aIdentifier
   * @return the document as Map of String/Object
   */
  public Map<String, Object> getDocument(@Nonnull final String aType,
      @Nonnull final String aIdentifier) {
    final GetResponse response = mClient.prepareGet(esConfig.getIndex(), aType, aIdentifier).execute()
        .actionGet();
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
  
  
  public boolean hasIndex(String aIndex){
    return mClient.admin().indices().prepareExists(aIndex).execute().actionGet()
        .isExists();
  }
  
  public void deleteIndex(String aIndex){
    
  }

}
