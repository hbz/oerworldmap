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
    Map<String, String> messages = new HashMap<>();
    ResourceBundle messageBundle = ResourceBundle.getBundle("messages", getLocale());
    for (String key : Collections.list(ResourceBundle.getBundle("messages", getLocale()).getKeys())) {
      try {
        String message = StringEscapeUtils.unescapeJava(new String(messageBundle.getString(key)
            .getBytes("ISO-8859-1"), "UTF-8"));
        messages.put(key, message);
      } catch (UnsupportedEncodingException e) {
        messages.put(key, messageBundle.getString(key));
      }
    }
    i18n.put("messages", messages);
    i18n.put("countries", Countries.map(getLocale()));
    i18n.put("languages", Languages.map(getLocale()));

    String countryMap = new ObjectMapper().convertValue(i18n, JsonNode.class).toString();
    return ok("window.i18nStrings = ".concat(countryMap)).as("application/javascript");
  }

}
