package helpers;

import com.github.jknack.handlebars.Options;
import controllers.OERWorldMap;
import play.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * @author fo
 */
public class HandlebarsHelpers {

  private OERWorldMap mController;

  public HandlebarsHelpers(OERWorldMap aController) {
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
      Logger.error("Error in Handlebars ifIn helper");
      return "";
    }
  }



  public CharSequence i18n(String key, Options options) {
    return _i18n(key, (String) options.hash.get("bundle"));
  }

  public CharSequence _i18n(String key, String bundle) {
    if (bundle != null) {
      try {
        return ResourceBundle.getBundle(bundle, mController.getLocale()).getString(key);
      } catch (MissingResourceException notInBundle) {
        return mController.getLabel(key);
      }
    }
    try {
      return ResourceBundle.getBundle("messages", mController.getLocale()).getString(key);
    } catch (MissingResourceException notMessage) {
      try {
        return ResourceBundle.getBundle("languages", mController.getLocale()).getString(key);
      } catch (MissingResourceException notLanguage) {
        try {
          return ResourceBundle.getBundle("countries", mController.getLocale()).getString(key);
        } catch (MissingResourceException notCountry) {
          try {
            return ResourceBundle.getBundle("labels", mController.getLocale()).getString(key);
          } catch (MissingResourceException notLabel) {
            try {
              return ResourceBundle.getBundle("iso3166-2", mController.getLocale()).getString(key);
            } catch (MissingResourceException notDivision) {
              try {
                return ResourceBundle.getBundle("ui", mController.getLocale()).getString(key);
              } catch (MissingResourceException notUi) {
                return mController.getLabel(key);
              }
            }
          }
        }
      }
    }
  }

}
