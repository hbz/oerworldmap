package helpers;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author fo
 */
public class Languages {

  public static Map<String,String> map(Locale aLocale) {
    Map<String,String> languageMap = new HashMap<>();
    for (String languageCode : Locale.getISOLanguages()) {
      Locale language = new Locale(languageCode);
      languageMap.put(language.getISO3Language(), language.getDisplayLanguage(aLocale));
    }
    return languageMap;
  }

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
