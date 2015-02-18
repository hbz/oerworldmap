package helpers;

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

    return countryList;

  }

  public static String getNameFor(String aCountryCode, Locale aLocale) {
    Locale country = new Locale("en", aCountryCode);
    return country.getDisplayCountry(aLocale);
  }

}
