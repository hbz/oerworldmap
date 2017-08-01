package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import helpers.JsonLdConstants;
import models.Record;
import models.Resource;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHits;
import play.Configuration;
import play.Environment;
import play.Logger;
import play.libs.Json;
import play.mvc.Result;
import services.QueryContext;
import services.repository.ElasticsearchRepository;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author pvb
 */
public class Reconciler extends OERWorldMap{

  public Reconciler(Configuration aConf, Environment aEnv) {
    super(aConf, aEnv);
  }


  public static Result meta(String callback) {
    ObjectNode result = Json.newObject();
    result.put("name", "oerworldmap reconciliation");
    result.put("identifierSpace", "https://oerworldmap.org/resource");
    result.put("schemaSpace", "https://oerworldmap.org/resource");
    result.set("defaultTypes", Json.toJson(Resource.mIdentifiedTypes));
    result.set("view", Json.newObject().put("url", "https://oerworldmap.org/resource/{{id}}"));
    return callback.isEmpty() ?
      ok(result) :
      ok(String.format("/**/%s(%s);", callback, result.toString())).as("application/json");
  }


  public Result reconcile(String aQueryTerm) {
    QueryContext queryContext = getQueryContext();
    ObjectNode response = Json.newObject();
      SearchResponse searchResponse = mBaseRepository.reconcile(aQueryTerm, 0, 10, null,
      null, queryContext);
      // TODO: parametrize dynamically
      List<JsonNode> results = mapToResults(searchResponse.getHits());
      ObjectNode resultsForInputQuery = Json.newObject();
      resultsForInputQuery.put("result", Json.toJson(results));
      Logger.debug("r: " + resultsForInputQuery);
      response.set(/*inputQuery.getKey()*/ "key", resultsForInputQuery);
    return ok(response);
  }

  private static List<JsonNode> mapToResults(SearchHits searchHits) {
    return Arrays.asList(searchHits.getHits()).stream().map(hit -> {
      Record map = Resource.fromMap();
      hit.getSource();
      ObjectNode resultForHit = Json.newObject();
      resultForHit.put("id", hit.getId());
      Object name = map.get();
      resultForHit.put("name", name == null ? "" : name + "");
      resultForHit.put("score", hit.getScore());
      resultForHit.put("type", map.get(JsonLdConstants.TYPE));
      return resultForHit;
    }).collect(Collectors.toList());
  }

}
