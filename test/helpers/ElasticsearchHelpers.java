package helpers;

import services.repository.ElasticsearchRepository;

import java.io.IOException;

public class ElasticsearchHelpers {

  // create a new clean ElasticsearchIndex for this Test class
  public static void cleanIndex(ElasticsearchRepository aEsRepo, String aIndex) throws IOException {
    if (aEsRepo.hasIndex(aIndex)) {
      aEsRepo.deleteIndex(aIndex);
    }
    aEsRepo.createIndex(aIndex);
  }
}
