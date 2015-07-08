package controllers;

import helpers.Countries;
import models.Record;
import models.Resource;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import play.Logger;
import play.mvc.Result;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fo
 */
public class CountryIndex extends OERWorldMap {

  public static Result read(String id) throws IOException {
    if (! Arrays.asList(java.util.Locale.getISOCountries()).contains(id.toUpperCase())) {
      return notFound("Not found");
    }

    String resource_field = Record.RESOURCEKEY + ".location.address.addressCountry";
    String mentions_field = Record.RESOURCEKEY + ".mentions.location.address.addressCountry";
    String provider_field = Record.RESOURCEKEY + ".provider.location.address.addressCountry";

    AggregationBuilder byCountry = AggregationBuilders
        .terms("by_country").script("doc['" + resource_field + "'].values + doc['" + mentions_field + "'].values  + doc['" + provider_field + "'].values").include(id).size(0)
        .subAggregation(AggregationBuilders
            .filter("organizations")
            .filter(FilterBuilders.termFilter(Record.RESOURCEKEY + ".@type", "Organization")))
        .subAggregation(AggregationBuilders
            .filter("users")
            .filter(FilterBuilders.termFilter(Record.RESOURCEKEY + ".@type", "Person")))
        .subAggregation(AggregationBuilders
            .filter("articles")
            .filter(FilterBuilders.termFilter(Record.RESOURCEKEY + ".@type", "Article")))
        .subAggregation(AggregationBuilders
            .filter("services")
            .filter(FilterBuilders.termFilter(Record.RESOURCEKEY + ".@type", "Service")))
        // TODO: The following implies that somebody can only be a chamption for her country. Is this correct?
        .subAggregation(AggregationBuilders
            .filter("champions")
            .filter(FilterBuilders.termFilter(Record.RESOURCEKEY + ".countryChampionFor", id)));

    //AggregationBuilder championsByCountry = AggregationBuilders.terms("champions_by_country").field(
        //Record.RESOURCEKEY + ".countryChampionFor").include(id).size(0);
    Resource countryAggregation = mBaseRepository.query(byCountry);

    List<Resource> champions = mBaseRepository.esQuery("countryChampionFor:".concat(id.toUpperCase()));
    List<Resource> resources = mBaseRepository.esQuery("about.\\*.addressCountry:".concat(id.toUpperCase()));
    Map<String,Object> scope = new HashMap<>();

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
