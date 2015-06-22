package controllers;

import models.Record;
import models.Resource;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import play.mvc.Result;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author fo
 */
public class AggregationIndex extends OERWorldMap {

  public static Result list() throws IOException {
    @SuppressWarnings("rawtypes")
    AggregationBuilder usersByCountry = AggregationBuilders.terms("users_by_country").field(
        Record.RESOURCEKEY + ".workLocation.address.addressCountry").size(0);
    // FIXME: Use filter to restrict to only organizations
    AggregationBuilder organizationsByCountry = AggregationBuilders.terms("organizations_by_country").field(
        Record.RESOURCEKEY + ".location.address.addressCountry").size(0);
    AggregationBuilder countryChampions = AggregationBuilders.terms("champions_by_country").field(
        Record.RESOURCEKEY + ".countryChampionFor").size(0);
    Resource countryAggregation = mBaseRepository.query(Arrays.asList(usersByCountry, organizationsByCountry, countryChampions));
    Map<String,Object> scope = new HashMap<>();
    scope.put("countryAggregation", countryAggregation);
    return ok(render("Country Aggregations", "AggregationIndex/index.mustache", scope));
  }

}
