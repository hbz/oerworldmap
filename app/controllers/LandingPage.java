package controllers;

import java.io.IOException;

import models.Resource;

import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;

import play.mvc.Result;


public class LandingPage extends OERWorldMap {

  public static Result get() throws IOException {

    @SuppressWarnings("rawtypes")
    AggregationBuilder aggregationBuilder = AggregationBuilders.terms("by_country").field(
        "workLocation.address.addressCountry").size(0);
    Resource countryAggregation = mBaseRepository.query(aggregationBuilder);
    mResponseData.put("countryAggregation", countryAggregation);
    return ok(render("OER World Map", "LandingPage/index.mustache"));

  }

}
