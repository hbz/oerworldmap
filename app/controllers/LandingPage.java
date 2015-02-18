package controllers;

import java.io.IOException;

import helpers.Countries;
import models.Resource;

import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;

import play.mvc.*;

import com.fasterxml.jackson.databind.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.net.URL;

public class LandingPage extends OERWorldMap {

  public static Result get() throws IOException {
    
    AggregationBuilder aggregationBuilder =
            AggregationBuilders.terms("by_country").field("address.countryName");
    Resource countryAggregation = resourceRepository.query(aggregationBuilder);

    Map<String, Object> data = new HashMap<>();

    ArrayList<HashMap> visionStatements;
    try {
      String spreadsheet = "18Q0Q4i50xTBAEZ4LNDEXLIvrB8oZ6dG0WHBhe9DxMDg";
      visionStatements = getGoogleData(spreadsheet, "2");
    } catch (IOException e) {
      visionStatements = new ArrayList<>();
    }

    data.put("visionStatements", visionStatements);
    data.put("countryAggregation", countryAggregation);
    return ok(render("Home", data, "LandingPage/index.mustache"));

  }

  private static ArrayList<HashMap> getGoogleData(String spreadsheet, String worksheet)
      throws IOException {

    ArrayList<HashMap> result = new ArrayList<>();
    URL url = new URL("https://spreadsheets.google.com/feeds/list/"
        + spreadsheet + "/" + worksheet + "/public/values?alt=json");
    ObjectMapper mapper = new ObjectMapper();
    LinkedHashMap<String,LinkedHashMap> json = mapper.readValue(url, LinkedHashMap.class);
    LinkedHashMap<String,ArrayList> feed = json.get("feed");
    ArrayList<LinkedHashMap> rows = feed.get("entry");

    for (LinkedHashMap<String,LinkedHashMap> row : rows) {
      HashMap<String,String> e = new HashMap<>();
      for (Map.Entry<String,LinkedHashMap> entry : row.entrySet()) {
        String key = entry.getKey();
        if (key.contains("$")) {
          e.put(key.split("\\$")[1], entry.getValue().get("$t").toString());
        }
      }
      result.add(e);
    }

    return result;

  }

}
