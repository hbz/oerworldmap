package services.repository;

import models.Commit;
import models.ModelCommon;

import java.io.IOException;
import java.util.List;

/**
 * Created by fo on 07.04.16.
 */
public interface Versionable {

  void commit(Commit aCommit) throws IOException;

  Commit.Diff getDiff(ModelCommon aResource);

  Commit.Diff getDiff(List<ModelCommon> aResources);

  ModelCommon stage(ModelCommon aResource) throws IOException;

  ModelCommon getItem(String aId, String aVersion);

  List<Commit> log(String aId);

}
