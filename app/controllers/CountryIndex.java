package controllers;

import helpers.Countries;
import models.Resource;
import play.mvc.Result;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fo
 */
public class CountryIndex extends OERWorldMap {

  public static Result read(String id) {
    if (! Arrays.asList(java.util.Locale.getISOCountries()).contains(id.toUpperCase())) {
      return notFound("Not found");
    }

    List<Resource> champions = mBaseRepository.esQuery("countryChampionFor:".concat(id.toUpperCase()));
    List<Resource> resources = mBaseRepository.esQuery("about.\\*.addressCountry:".concat(id.toUpperCase()));
    Map<String,Object> scope = new HashMap<>();

    scope.put("alpha-2", id.toUpperCase());
    scope.put("name", Countries.getNameFor(id, currentLocale));
    scope.put("champions", champions);
    scope.put("resources", resources);

    if (request().accepts("text/html")) {
      return ok(render("Home", "CountryIndex/read.mustache", scope));
    } else {
      return ok(resources.toString()).as("application/json");
    }

  }
}
