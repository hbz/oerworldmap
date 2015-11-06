package helpers;

import java.util.Locale;

/**
 * @author fo
 */
public class Languages {

  public static String propertyString(Locale aLocale) {
    String propertyString = "";
    for (String languageCode : Locale.getISOLanguages()) {
      Locale language = new Locale(languageCode);
      propertyString = propertyString.concat(language.getISO3Language()).concat("=")
          .concat(language.getDisplayLanguage(aLocale)).concat("\n");
    }
    return propertyString;
  }

}
