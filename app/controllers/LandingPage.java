package controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Record;
import models.Resource;

import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;

import play.mvc.Result;


public class LandingPage extends OERWorldMap {

  public static Result get() throws IOException {

    @SuppressWarnings("rawtypes")
    AggregationBuilder aggregationBuilder = AggregationBuilders.terms("by_country").field(
        Record.RESOURCEKEY + ".workLocation.address.addressCountry").size(0);
    Resource countryAggregation = mBaseRepository.query(aggregationBuilder);
    Map<String,Object> scope = new HashMap<>();
    scope.put("countryAggregation", countryAggregation);
    return ok(render("Home", "LandingPage/index.mustache", scope));

  }

}
