package controllers;

import helpers.Countries;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Record;
import models.Resource;

import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;

import play.Logger;
import play.mvc.Result;
import services.AggregationProvider;

/**
 * @author fo
 */
public class CountryIndex extends OERWorldMap {

  public static Result read(String id) throws IOException {
    if (!Arrays.asList(java.util.Locale.getISOCountries()).contains(id.toUpperCase())) {
      return notFound("Not found");
    }

    Resource countryAggregation = mBaseRepository.aggregate(AggregationProvider.getForCountryAggregation(id));
    List<Resource> champions = mBaseRepository.query(
        Record.RESOURCEKEY + ".countryChampionFor:".concat(id.toUpperCase()), null);
    List<Resource> resources = mBaseRepository.query(
        Record.RESOURCEKEY + ".\\*.addressCountry:".concat(id.toUpperCase()), null);
    Map<String, Object> scope = new HashMap<>();

    scope.put("alpha-2", id.toUpperCase());
    scope.put("name", Countries.getNameFor(id, currentLocale));
    scope.put("champions", champions);
    scope.put("resources", resources);
    scope.put("countryAggregation", countryAggregation);

    if (request().accepts("text/html")) {
      return ok(render(Countries.getNameFor(id, currentLocale), "CountryIndex/read.mustache", scope));
    } else {
      return ok(resources.toString()).as("application/json");
    }

  }
}
