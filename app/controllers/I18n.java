package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import helpers.Countries;
import helpers.Languages;
import org.apache.commons.lang3.StringEscapeUtils;
import play.mvc.Result;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * @author fo
 */
public class I18n extends OERWorldMap {

  public static Result get() {
    Map<String, Object> i18n = new HashMap<>();
    Map<String, String> messages = new HashMap<>();
    ResourceBundle messageBundle = ResourceBundle.getBundle("messages", OERWorldMap.mLocale);
    for (String key : Collections.list(ResourceBundle.getBundle("messages", OERWorldMap.mLocale).getKeys())) {
      try {
        String message = StringEscapeUtils.unescapeJava(new String(messageBundle.getString(key)
            .getBytes("ISO-8859-1"), "UTF-8"));
        messages.put(key, message);
      } catch (UnsupportedEncodingException e) {
        messages.put(key, messageBundle.getString(key));
      }
    }
    i18n.put("messages", messages);
    i18n.put("countries", Countries.map(OERWorldMap.mLocale));
    i18n.put("languages", Languages.map(OERWorldMap.mLocale));

    String countryMap = new ObjectMapper().convertValue(i18n, JsonNode.class).toString();
    return ok("i18nStrings = ".concat(countryMap)).as("application/javascript");
  }

}
