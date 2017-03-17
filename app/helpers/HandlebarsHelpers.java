package helpers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.github.jknack.handlebars.Options;

import controllers.OERWorldMap;
import models.Resource;
import play.Logger;

/**
 * @author fo
 */
public class HandlebarsHelpers {

  private static OERWorldMap mController;

  public static void setController(OERWorldMap aController) {
    mController = aController;
  }

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
    return _i18n(key, (String) options.hash.get("bundle"));
  }

  public static CharSequence _i18n(String key, String bundle) {
    if (bundle != null) {
      try {
        return ResourceBundle.getBundle(bundle).getString(key);
      } catch (MissingResourceException notInBundle) {
        return mController.getLabel(key);
      }
    }
    try {
      return ResourceBundle.getBundle("messages").getString(key);
    } catch (MissingResourceException notMessage) {
      try {
        return ResourceBundle.getBundle("languages").getString(key);
      } catch (MissingResourceException notLanguage) {
        try {
          return ResourceBundle.getBundle("countries").getString(key);
        } catch (MissingResourceException notCountry) {
          try {
            return ResourceBundle.getBundle("labels").getString(key);
          } catch (MissingResourceException notLabel) {
            return mController.getLabel(key);
          }
        }
      }
    }
  }

  public static CharSequence getUser(String aAccount, Options options) throws IOException {
    Resource user = mController.getUser(aAccount);
    return user != null ? options.fn(user) : options.inverse();
  }

}
