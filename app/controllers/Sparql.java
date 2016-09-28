package controllers;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.TemplateLoader;
import helpers.ResourceTemplateLoader;
import models.Commit;
import models.TripleCommit;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import play.Logger;
import play.mvc.Result;
import play.twirl.api.Html;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by fo on 21.07.16.
 */
public class Sparql extends OERWorldMap  {

  private static TemplateLoader loader;
  private static Handlebars handlebars;
  static {
    loader = new ResourceTemplateLoader();
    loader.setPrefix("public/mustache");
    loader.setSuffix("");
    handlebars = new Handlebars(loader);
  }

  public Result query(String q) throws IOException {

    Map<String, Object> mustacheData = new HashMap<>();
    if (! StringUtils.isEmpty(q)) {
      mustacheData.put("q", q);
      mustacheData.put("result", mBaseRepository.sparql(q));
    }

    Template template = handlebars.compile("Sparql/query.mustache");
    return ok(Html.apply(template.apply(mustacheData))).as("text/html");

  }

  public Result update(String delete, String insert, String where) throws IOException {

    Map<String, Object> mustacheData = new HashMap<>();
    mustacheData.put("delete", delete);
    mustacheData.put("insert", insert);
    mustacheData.put("where", where);
    mustacheData.put("diff", mBaseRepository.update(delete, insert, where));

    Template template = handlebars.compile("Sparql/update.mustache");
    return ok(Html.apply(template.apply(mustacheData))).as("text/html");

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
