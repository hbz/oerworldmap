package controllers;

import helpers.Countries;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Record;
import models.Resource;

import models.ResourceList;
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
    ResourceList champions = mBaseRepository.query(
        Record.RESOURCEKEY + ".countryChampionFor:".concat(id.toUpperCase()), 0, 9999, null);
    ResourceList resources = mBaseRepository.query(
        Record.RESOURCEKEY + ".\\*.addressCountry:".concat(id.toUpperCase()), 0, 9999, null);
    Map<String, Object> scope = new HashMap<>();

    scope.put("alpha-2", id.toUpperCase());
    scope.put("name", Countries.getNameFor(id, currentLocale));
    scope.put("champions", champions.getItems());
    scope.put("resources", resources.toResource());
    scope.put("countryAggregation", countryAggregation);

    if (request().accepts("text/html")) {
      return ok(render(Countries.getNameFor(id, currentLocale), "CountryIndex/read.mustache", scope));
    } else {
      return ok(resources.toString()).as("application/json");
    }

  }
}
