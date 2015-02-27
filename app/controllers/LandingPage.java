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
    Resource countryAggregation = mResourceRepository.query(aggregationBuilder);

    ResourceBundle countryChampionsProperties = ResourceBundle.getBundle("CountryChampionsBundle");
    List<Map<String,String>> countryChampions = new ArrayList<>();
    for (String key : Collections.list(countryChampionsProperties.getKeys())) {
      Map<String,String> countryChampion = new HashMap<>();
      countryChampion.put("countryCode", key);
      countryChampion.put("countryName", Countries.getNameFor(key, currentLocale));
      countryChampions.add(countryChampion);
    }

    List<HashMap<String, String>> visionStatements;
    try {
      String spreadsheet = "18Q0Q4i50xTBAEZ4LNDEXLIvrB8oZ6dG0WHBhe9DxMDg";
      visionStatements = getGoogleData(spreadsheet, "2");
    } catch (IOException e) {
      visionStatements = new ArrayList<>();
    }

    mResponseData.put("countriesWithChampions", countryChampions);
    mResponseData.put("visionStatements", visionStatements);
    mResponseData.put("countryAggregation", countryAggregation);
    return ok(render("Home", "LandingPage/index.mustache"));

  }

  private static List<HashMap<String, String>> getGoogleData(String spreadsheet, String worksheet)
      throws IOException {

    List<HashMap<String, String>> result = new ArrayList<HashMap<String, String>>();
    URL url = new URL("https://spreadsheets.google.com/feeds/list/" + spreadsheet + "/" + worksheet
        + "/public/values?alt=json");
    ObjectMapper mapper = new ObjectMapper();
    LinkedHashMap<String, LinkedHashMap<String, ArrayList<LinkedHashMap<String, LinkedHashMap<String, String>>>>> json //
    = mapper.readValue(url, LinkedHashMap.class);
    LinkedHashMap<String, ArrayList<LinkedHashMap<String, LinkedHashMap<String, String>>>> feed = json
        .get("feed");
    ArrayList<LinkedHashMap<String, LinkedHashMap<String, String>>> rows = feed.get("entry");

    for (LinkedHashMap<String, LinkedHashMap<String, String>> row : rows) {
      HashMap<String, String> e = new HashMap<String, String>();
      for (Map.Entry<String, LinkedHashMap<String, String>> entry : row.entrySet()) {
        String key = entry.getKey();
        if (key.contains("$")) {
          e.put(key.split("\\$")[1], entry.getValue().get("$t").toString());
          Logger.error(entry.getValue().getClass().getName());
        }
      }
      result.add(e);
    }

    return result;

  }

}
