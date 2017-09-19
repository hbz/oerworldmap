package helpers;

import services.repository.ElasticsearchRepository;

public class ElasticsearchHelpers {

  // create a new clean ElasticsearchIndex for this Test class
  public static void deleteIndex(ElasticsearchRepository aEsRepo, String aIndex) {
    if (aEsRepo.hasIndex(aIndex)) {
      aEsRepo.deleteIndex(aIndex);
    }
  }
}
