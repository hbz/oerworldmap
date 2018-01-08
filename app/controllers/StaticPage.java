package controllers;

import java.util.Map;

import play.Configuration;
import play.Environment;
import play.mvc.Result;

import javax.inject.Inject;

/**
 * @author fo
 */
public class StaticPage extends OERWorldMap {

  @Inject
  public StaticPage(Configuration aConf, Environment aEnv) {
    super(aConf, aEnv);
  }

  public Result get(String aPage) {

    Map<String, String> page = mPageProvider.getPage(aPage, getLocale());
    if (page == null) {
      return notFound("Page not found");
    } else {
      return ok(render(page.get("title"), "StaticPage/index.mustache", (Map) page));
    }

  }

}
