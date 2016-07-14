package controllers;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.pegdown.PegDownProcessor;

import play.Play;
import play.mvc.Result;

/**
 * @author fo
 */
public class StaticPage extends OERWorldMap {

  public static Result get(String aPage) {

    String title = aPage.substring(0, 1).toUpperCase().concat(aPage.substring(1));
    String language = OERWorldMap.mLocale.getLanguage();
    String country = OERWorldMap.mLocale.getCountry();
    String extension = ".md";
    String path = "public/pages/";
    ClassLoader classLoader = Play.application().classloader();
    String titleLocalePath = path.concat(title).concat("_").concat(language).concat("_")
        .concat(country).concat(extension);
    String titleLanguagePath = path.concat(title).concat("_").concat(language).concat(extension);
    String titlePath = path.concat(title).concat(extension);
    String body;

    InputStream inputStream;
    try {
      inputStream = classLoader.getResourceAsStream(titleLocalePath);
      body = IOUtils.toString(inputStream);
      inputStream.close();
    } catch (NullPointerException | IOException noLocale) {
      try {
        inputStream = classLoader.getResourceAsStream(titleLanguagePath);
        body = IOUtils.toString(inputStream);
        inputStream.close();
      } catch (NullPointerException | IOException noLanguage) {
        try {
          inputStream = classLoader.getResourceAsStream(titlePath);
          body = IOUtils.toString(inputStream);
          inputStream.close();
        } catch (NullPointerException | IOException noPage) {
          return notFound("Page not found");
        }
      }
    }

    PegDownProcessor pegDownProcessor = new PegDownProcessor();
    Map<String, Object> scope = new HashMap<>();
    scope.put("title", title);
    scope.put("body", pegDownProcessor.markdownToHtml(body));
    return ok(render(title, "StaticPage/index.mustache", scope));
  }

}
