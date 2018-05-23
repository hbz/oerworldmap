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

  private final static String mQueryTemplate =
    "<!DOCTYPE html>" +
      "<html class='no-js'>" +
      "    <head>" +
      "        <meta charset='utf-8' />" +
      "        <title>OER World Map - SPARQL</title>" +
      "    </head>" +
      "    <body>" +
      "        <h1>SPARQL</h1>" +
      "        <form method='get' action='/sparql/query'>" +
      "            <textarea style='width: 100%%' name='q'>%s</textarea>" +
      "            <button type='submit'>Query</button>" +
      "            <pre>%s</pre>" +
      "        </form>" +
      "    </body>" +
      "</html>";

  private final static String mUpdateTemplate =
    "<!DOCTYPE html>" +
      "<html class='no-js'>" +
      "    <head>" +
      "        <meta charset='utf-8' />" +
      "        <title>OER World Map - SPARQL UPDATE</title>" +
      "    </head>" +
      "    <body>" +
      "        <h1><a href='https://www.w3.org/TR/sparql11-update/#deleteInsert'>SPARQL DELETE/INSERT</a></h1>" +
      "        <form method='get' action='/sparql/update'>" +
      "            <label>DELETE {<input style='width:100%%' name='delete' value='%s'>}</label><br>" +
      "            <label>INSERT {<input style='width:100%%' name='insert' value='%s'>}</label><br>" +
      "            <label>WHERE {<input style='width:100%%' name='where' value='%s'>}</label><br>" +
      "            <p><button type='submit'>Generate Diff</button></p>" +
      "        </form>" +
      "        <h1>PATCH</h1>" +
      "        <form method='post' action='/sparql/patch'>" +
      "            <textarea style='width:100%%;' rows='25' name='diff'>%s</textarea><br>" +
      "            <p><button type='submit'>Apply Patch</button></p>" +
      "        </form>" +
      "    </body>" +
      "</html>";

  @Inject
  public Sparql(Configuration aConf, Environment aEnv) {
    super(aConf, aEnv);
  }

  public Result query(String q) throws IOException {

    return ok(StringUtils.isEmpty(q)
      ? String.format(mQueryTemplate, "", "")
      : String.format(mQueryTemplate, q, mBaseRepository.sparql(q))
    ).as("text/html");

  }

  public Result update(String delete, String insert, String where) throws IOException {

    return ok(String.format(mUpdateTemplate, delete, insert, where, mBaseRepository.update(delete, insert, where)))
      .as("text/html");

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
