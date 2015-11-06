package helpers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.github.jknack.handlebars.Options;

import controllers.OERWorldMap;
import play.Logger;

/**
 * @author fo
 */
public class HandlebarsHelpers {

  public CharSequence ifIn(String filter, String value, Map<String, ArrayList<String>> filters,
                           Options options) {
    try {
      ArrayList<String> values = filters.get(filter);
      if (!(null == values))
        for (String member : values) {
          if (member.equals(value)) {
            return options.fn();
          }
        }
      return options.inverse();
    } catch (IOException e) {
      Logger.error(e.toString());
      return "";
    }
  }

  public CharSequence unlessIn(String filter, String value, Map<String, ArrayList<String>> filters,
                               Options options) {
    try {
      ArrayList<String> values = filters.get(filter);
      if (!(null == values))
        for (String member : values) {
          if (member.equals(value)) {
            return options.inverse();
          }
        }
      return options.fn();
    } catch (IOException e) {
      Logger.error(e.toString());
      return "";
    }
  }

  public CharSequence i18n(String key, Options options) {
    return _i18n(key);
  }

  public static CharSequence _i18n(String key) {
    try {
      return ResourceBundle.getBundle("messages").getString(key);
    } catch (MissingResourceException notMessage) {
      try {
        return ResourceBundle.getBundle("languages").getString(key);
      } catch (MissingResourceException notLanguage) {
        try {
          return ResourceBundle.getBundle("countries").getString(key);
        } catch (MissingResourceException notCountry) {
          return OERWorldMap.getLabel(key);
        }
      }
    }
  }

}
