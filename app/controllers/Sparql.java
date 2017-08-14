package controllers;

import models.Commit;
import models.TripleCommit;
import org.apache.commons.lang3.StringUtils;
import play.Configuration;
import play.Environment;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;
import java.time.ZonedDateTime;

/**
 * Created by fo on 21.07.16.
 */
public class Sparql extends OERWorldMap  {

  @Inject
  public Sparql(Configuration aConf, Environment aEnv) {
    super(aConf, aEnv);
  }

  public Result query(String q) throws IOException {

    return StringUtils.isEmpty(q) ? ok("") : ok(mBaseRepository.sparql(q));

  }

  public Result update(String delete, String insert, String where) throws IOException {

    return ok (mBaseRepository.update(delete, insert, where));

  }

  public Result patch() throws IOException {

    String diffString = ctx().request().body().asFormUrlEncoded().get("diff")[0];

    Commit.Diff diff = TripleCommit.Diff.fromString(diffString);
    TripleCommit.Header header = new TripleCommit.Header(getMetadata().get(TripleCommit.Header.AUTHOR_HEADER),
      ZonedDateTime.parse(getMetadata().get(TripleCommit.Header.DATE_HEADER)));

    Commit commit = new TripleCommit(header, diff);

    mBaseRepository.commit(commit);

    return ok(commit.toString());

  }


}
