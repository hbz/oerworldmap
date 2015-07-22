package controllers;

import helpers.UniversalFunctions;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.pegdown.PegDownProcessor;
import play.Logger;
import play.Play;
import play.mvc.Result;

import java.io.IOException;
import java.io.InputStream;
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
    String path = "public/pages/";
    ClassLoader classLoader = Play.application().classloader();
    String titleLocalePath = path.concat(title).concat("_").concat(language).concat("_").concat(country)
        .concat(extension);
    String titleLanguagePath = path.concat(title).concat("_").concat(language).concat(extension);
    String titlePath = path.concat(title).concat(extension);
    String body;

    try {
      body = IOUtils.toString(classLoader.getResourceAsStream(titleLocalePath));
    } catch (NullPointerException | IOException noLocale) {
      try {
        body = IOUtils.toString(classLoader.getResourceAsStream(titleLanguagePath));
      } catch (NullPointerException | IOException noLanguage) {
        try {
          body = IOUtils.toString(classLoader.getResourceAsStream(titlePath));
        } catch (NullPointerException | IOException noPage) {
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
