package controllers;

import helpers.Countries;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import models.Record;
import models.Resource;

import models.ResourceList;

import play.mvc.Result;
import services.AggregationProvider;
import services.QueryContext;

/**
 * @author fo
 */
public class CountryIndex extends OERWorldMap {

  public static Result read(String id, boolean embed) throws IOException {
    if (!Arrays.asList(java.util.Locale.getISOCountries()).contains(id.toUpperCase())) {
      return notFound("Not found");
    }

    QueryContext queryContext = (QueryContext) ctx().args.get("queryContext");

    queryContext.setFetchSource(new String[]{
      "about.@id", "about.@type", "about.name", "about.alternateName", "about.location", "about.image",
      "about.provider.@id", "about.provider.@type", "about.provider.name", "about.provider.location",
      "about.participant.@id", "about.participant.@type", "about.participant.name", "about.participant.location",
      "about.agent.@id", "about.agent.@type", "about.agent.name", "about.agent.location",
      "about.mentions.@id", "about.mentions.@type", "about.mentions.name", "about.mentions.location",
      "about.mainEntity.@id", "about.mainEntity.@type", "about.mainEntity.name", "about.mainEntity.location"
    });

    Resource countryAggregation = mBaseRepository.aggregate(AggregationProvider.getForCountryAggregation(id.toUpperCase()));
    ResourceList champions = mBaseRepository.query(
        Record.RESOURCEKEY + ".countryChampionFor:".concat(id.toUpperCase()), 0, 9999, null, null);
    ResourceList resources = mBaseRepository.query(
        Record.RESOURCEKEY + ".location.address.addressCountry:".concat(id.toUpperCase()), 0, 9999, null, null,
            queryContext);
    Map<String, Object> scope = new HashMap<>();

    scope.put("alpha-2", id.toUpperCase());
    scope.put("name", Countries.getNameFor(id, currentLocale));
    scope.put("champions", champions.getItems());
    scope.put("resources", resources.toResource());
    scope.put("countryAggregation", countryAggregation);
    scope.put("embed", embed);

    if (request().accepts("text/html")) {
      return ok(render(Countries.getNameFor(id, currentLocale), "CountryIndex/read.mustache", scope));
    } else {
      return ok(resources.toString()).as("application/json");
    }

  }
}
