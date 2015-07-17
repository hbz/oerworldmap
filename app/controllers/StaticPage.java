package controllers;

import helpers.UniversalFunctions;
import org.apache.commons.lang3.StringEscapeUtils;
import org.pegdown.PegDownProcessor;
import play.Logger;
import play.mvc.Result;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * @author fo
 */
public class StaticPage extends OERWorldMap {

  public static Result get(String aPage)  {

    String title = aPage.substring(0, 1).toUpperCase().concat(aPage.substring(1));
    String language = currentLocale.getLanguage();
    String country = currentLocale.getCountry();
    String extension = ".md";
    String path = play.Play.application().path().getAbsolutePath() + "/data/pages/";
    String body;

    try {
      body = UniversalFunctions.readFile(path.concat(title.concat("_").concat(language).concat("_").concat(country)
          .concat(extension)), StandardCharsets.UTF_8);
    } catch (IOException noLocale) {
      try {
        body = UniversalFunctions.readFile(path.concat(title.concat("_").concat(language).concat(extension)),
            StandardCharsets.UTF_8);
      } catch (IOException noLang) {
        try {
          body = UniversalFunctions.readFile(path.concat(title.concat(extension)), StandardCharsets.UTF_8);
        } catch (IOException noPage) {
          return notFound("Page not found");
        }
      }
    }

    PegDownProcessor pegDownProcessor = new PegDownProcessor();
    Map<String,Object> scope = new HashMap<>();
    scope.put("title", title);
    scope.put("body", pegDownProcessor.markdownToHtml(body));
    return ok(render(title, "StaticPage/index.mustache", scope));
  }

}
