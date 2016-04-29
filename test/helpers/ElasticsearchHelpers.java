package helpers;

import services.repository.ElasticsearchRepository;

public class ElasticsearchHelpers {

  // create a new clean ElasticsearchIndex for this Test class
  public static void cleanIndex(ElasticsearchRepository aEsRepo, String aIndex) {
    if (aEsRepo.hasIndex(aIndex)) {
      aEsRepo.deleteIndex(aIndex);
    }
    aEsRepo.createIndex(aIndex);
  }
}
