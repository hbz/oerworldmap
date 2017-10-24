package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import helpers.UniversalFunctions;
import play.Configuration;
import play.Environment;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * @author fo
 */
public class I18n extends OERWorldMap {

  private static ObjectMapper mObjectMapper = new ObjectMapper();

  private String[] mBundles = {
    "ui", "iso3166-1-alpha-2", "iso3166-1-alpha-3", "iso3166-2", "iso639-1", "iso639-2"
  };

  @Inject
  public I18n(Configuration aConf, Environment aEnv) {
    super(aConf, aEnv);
  }

  public Result get() {

    Map<String, Object> i18n = new HashMap<>();

    for (String bundleName : mBundles) {
      Map<String, String> strings = UniversalFunctions.resourceBundleToMap(
        ResourceBundle.getBundle(bundleName, getLocale()));
      i18n.put(bundleName, strings);
    }

    String i18nMap = mObjectMapper.convertValue(i18n, JsonNode.class).toString();
    return ok("window.i18nStrings = ".concat(i18nMap)).as("application/javascript");

  }

}
