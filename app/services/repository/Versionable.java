package services.repository;

import models.Commit;
import models.Resource;

import java.io.IOException;

/**
 * Created by fo on 07.04.16.
 */
public interface Versionable {

  void commit(Commit aCommit) throws IOException;

  Commit.Diff getDiff(Resource aResource);

  Resource stage(Resource aResource) throws IOException;

}
