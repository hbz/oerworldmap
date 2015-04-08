package helpers;

import services.ElasticsearchClient;

public class ElasticsearchHelpers {
  
  // create a new clean ElasticsearchIndex for this Test class
  public static void cleanIndex(ElasticsearchClient aEsClient, String aIndex) {
    if (aEsClient.hasIndex(aIndex)) {
      aEsClient.deleteIndex(aIndex);
    }
    aEsClient.createIndex(aIndex);
  }
}
