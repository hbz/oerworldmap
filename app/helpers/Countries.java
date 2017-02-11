package helpers;

import java.text.Collator;
import java.util.*;

/**
 * @author fo
 */
public class Countries {

  public static List<Map<String,String>> list(Locale aLocale) {

    List<Map<String,String>> countryList = new ArrayList<>();

    for (String countryCode : Locale.getISOCountries()) {
      Locale country = new Locale("en", countryCode);
      Map<String, String> entry = new HashMap<>();
      entry.put("name", country.getDisplayCountry(aLocale));
      entry.put("alpha-2", country.getCountry());
      countryList.add(entry);
    }

    final Collator collator = Collator.getInstance(aLocale);
    Collections.sort(countryList, new Comparator<Map<String, String>>() {
      @Override
      public int compare(Map<String, String> o1, Map<String, String> o2) {
        return collator.compare(o1.get("name"), o2.get("name"));
      }
    });

    return countryList;

  }

  public static List<String> synonyms() {
    List<String> countries = new ArrayList<>();
    for (String countryCode : Locale.getISOCountries()) {
      Locale locale = new Locale("en", countryCode);
      Set<String> country = new HashSet<>();
      for (String languageCode : Locale.getISOLanguages()) {
        country.add(locale.getDisplayCountry(new Locale(languageCode)));
      }
      countries.add(countryCode.concat(",").concat(String.join(",", country)).toLowerCase());
    }
    return countries;
  }

  public static Map<String,String> map(Locale aLocale) {
    Map<String,String> countryMap = new HashMap<>();
    for (String countryCode : Locale.getISOCountries()) {
      Locale country = new Locale("en", countryCode);
      countryMap.put(country.getCountry(), country.getDisplayCountry(aLocale));
    }
    return countryMap;
  }

  public static String propertyString(Locale aLocale) {
    String propertyString = "";
    for (String countryCode : Locale.getISOCountries()) {
      Locale country = new Locale("en", countryCode);
      propertyString = propertyString.concat(country.getCountry()).concat("=")
          .concat(country.getDisplayCountry(aLocale)).concat("\n");
    }
    return propertyString;
  }

  public static String getNameFor(String aCountryCode, Locale aLocale) {
    Locale country = new Locale("en", aCountryCode);
    return country.getDisplayCountry(aLocale);
  }

}
