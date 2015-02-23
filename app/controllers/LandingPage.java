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
        "address.countryName");
    Resource countryAggregation = resourceRepository.query(aggregationBuilder);

    ResourceBundle countryChampionsProperties = ResourceBundle.getBundle("CountryChampionsBundle");
    List<Map<String,String>> countryChampions = new ArrayList<>();
    for (String key : Collections.list(countryChampionsProperties.getKeys())) {
      Map<String,String> countryChampion = new HashMap<>();
      countryChampion.put("countryCode", key);
      countryChampion.put("countryName", Countries.getNameFor(key, currentLocale));
      countryChampions.add(countryChampion);
    }

    ResourceBundle productVisionsProperties = ResourceBundle.getBundle("ProductVisionsBundle");
    List<Map<String,String>> productVisions = new ArrayList<>();
    for (String key : Collections.list(productVisionsProperties.getKeys())) {
      Map<String,String> productVision = new HashMap<>();
      productVision.put("statement", productVisionsProperties.getString(key));
      productVisions.add(productVision);
    }

    Map<String, Object> data = new HashMap<>();
    data.put("countriesWithChampions", countryChampions);
    data.put("productVisions", productVisions);
    data.put("countryAggregation", countryAggregation);
    return ok(render("Home", data, "LandingPage/index.mustache"));

  }

}
