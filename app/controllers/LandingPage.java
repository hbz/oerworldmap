package controllers;

import java.io.IOException;

import helpers.Countries;
import models.Resource;

import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;

import play.Logger;
import play.mvc.*;

import com.fasterxml.jackson.databind.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ResourceBundle;
import java.net.URL;

public class LandingPage extends OERWorldMap {

  public static Result get() throws IOException {

    AggregationBuilder aggregationBuilder = AggregationBuilders.terms("by_country").field(
        "workLocation.address.addressCountry");
    Resource countryAggregation = mResourceRepository.query(aggregationBuilder);
    mResponseData.put("countryAggregation", countryAggregation);
    return ok(render("Home", "LandingPage/index.mustache"));

  }

}
