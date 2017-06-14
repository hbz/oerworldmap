package controllers;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.util.options.MutableDataHolder;
import com.vladsch.flexmark.util.options.MutableDataSet;
import org.apache.commons.io.IOUtils;
import com.vladsch.flexmark.parser.Parser;
import play.Configuration;
import play.Environment;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author fo
 */
public class StaticPage extends OERWorldMap {

  @Inject
  public StaticPage(Configuration aConf, Environment aEnv) {
    super(aConf, aEnv);
  }

  public Result get(String aPage) {

    String title = aPage.substring(0, 1).toUpperCase().concat(aPage.substring(1));
    String language = getLocale().getLanguage();
    String country = getLocale().getCountry();
    String extension = ".md";
    String path = "public/pages/";
    ClassLoader classLoader = mEnv.classLoader();
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

    MutableDataHolder options = new MutableDataSet();
    options.setFrom(ParserEmulationProfile.GITHUB_DOC);
    Parser parser = Parser.builder(options).build();
    HtmlRenderer renderer = HtmlRenderer.builder().build();
    Map<String, Object> scope = new HashMap<>();
    scope.put("title", title);
    scope.put("body", renderer.render(parser.parse(body)));
    return ok(render(title, "StaticPage/index.mustache", scope));
  }

}
