package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import helpers.Countries;
import helpers.Languages;
import org.apache.commons.lang3.StringEscapeUtils;
import play.Configuration;
import play.Environment;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * @author fo
 */
public class I18n extends OERWorldMap {

  @Inject
  public I18n(Configuration aConf, Environment aEnv) {
    super(aConf, aEnv);
  }

  public Result get() {
    Map<String, Object> i18n = new HashMap<>();

    for (String bundleName : new String[]{"messages", "iso3166-2", "ui"}) {
      Map<String, String> strings = new HashMap<>();
      ResourceBundle bundle = ResourceBundle.getBundle(bundleName, getLocale());
      for (String key : Collections.list(ResourceBundle.getBundle(bundleName, getLocale()).getKeys())) {
        try {
          String message = StringEscapeUtils.unescapeJava(new String(bundle.getString(key)
            .getBytes("ISO-8859-1"), "UTF-8"));
          strings.put(key, message);
        } catch (UnsupportedEncodingException e) {
          strings.put(key, bundle.getString(key));
        }
      }
      i18n.put(bundleName, strings);
    }


    i18n.put("countries", Countries.map(getLocale()));
    i18n.put("languages", Languages.map(getLocale()));

    String countryMap = new ObjectMapper().convertValue(i18n, JsonNode.class).toString();
    return ok("window.i18nStrings = ".concat(countryMap)).as("application/javascript");
  }

}
