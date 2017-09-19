package helpers;

import services.repository.ElasticsearchRepository;

public class ElasticsearchHelpers {

  // create a new clean ElasticsearchIndex for this Test class
  public static void cleanIndex(final ElasticsearchRepository aEsRepo, final String aIndex,
                                final String aMappingFile) {
    if (aEsRepo.hasIndex(aIndex)) {
      aEsRepo.deleteIndex(aIndex);
    }
    aEsRepo.createIndex(aIndex, aMappingFile);
  }
}
