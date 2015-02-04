package controllers;

import java.io.IOException;

import models.Resource;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;

import play.*;
import play.mvc.*;

import com.fasterxml.jackson.databind.*;

import services.ElasticsearchClient;
import services.ElasticsearchConfig;
import services.ElasticsearchRepository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.net.URL;

public class LandingPage extends Controller {

  private static Settings clientSettings = ImmutableSettings.settingsBuilder()
      .put(new ElasticsearchConfig().getClientSettings()).build();
  private static Client mClient = new TransportClient(clientSettings)
      .addTransportAddress(new InetSocketTransportAddress(new ElasticsearchConfig().getServer(),
          9300));
  private static ElasticsearchClient mElasticsearchClient = new ElasticsearchClient(mClient);
  private static ElasticsearchRepository resourceRepository = new ElasticsearchRepository(mElasticsearchClient);

  public static Result get() throws IOException {
    
    AggregationBuilder aggregationBuilder =
            AggregationBuilders.terms("by_country").field("address.countryName");
    Resource countryAggregation = resourceRepository.query(aggregationBuilder).get(0);
    
    ArrayList countryChampions;
    ArrayList visionStatements;
    try {
      String spreadsheet = "18Q0Q4i50xTBAEZ4LNDEXLIvrB8oZ6dG0WHBhe9DxMDg";
      countryChampions = getGoogleData(spreadsheet, "1");
      visionStatements = getGoogleData(spreadsheet, "2");
    } catch (IOException e) {
      countryChampions = new ArrayList();
      visionStatements = new ArrayList();
    }

    return ok(views.html.LandingPage.index.render(visionStatements, countryChampions, countryAggregation));

  }

  private static ArrayList<HashMap> getGoogleData(String spreadsheet, String worksheet)
      throws IOException {

    ArrayList<HashMap> result = new ArrayList();
    URL url = new URL("https://spreadsheets.google.com/feeds/list/"
        + spreadsheet + "/" + worksheet + "/public/values?alt=json");
    ObjectMapper mapper = new ObjectMapper();
    LinkedHashMap<String,LinkedHashMap> json = mapper.readValue(url, LinkedHashMap.class);
    LinkedHashMap<String,ArrayList> feed = json.get("feed");
    ArrayList<LinkedHashMap> rows = feed.get("entry");

    for (LinkedHashMap<String,LinkedHashMap> row : rows) {
      HashMap<String,String> e = new HashMap<String,String>();
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
