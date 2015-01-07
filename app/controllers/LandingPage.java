package controllers;

import java.io.IOException;
import play.*;
import play.mvc.*;
import com.fasterxml.jackson.databind.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.net.URL;

public class LandingPage extends Controller {

  public static Result get() {

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

    return ok(views.html.LandingPage.index.render(visionStatements, countryChampions));

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
          System.out.println(entry.getValue().get("$t").getClass());
          e.put(key.split("\\$")[1], entry.getValue().get("$t").toString());
        }
      }
      result.add(e);
    }

    return result;

  }

}
