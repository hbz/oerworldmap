package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.Resource;
import play.Configuration;
import play.Environment;
import play.libs.Json;
import play.mvc.Result;
import services.QueryContext;

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
    final JsonNode reconciled = mBaseRepository.reconcile(aQueryTerm, 0, 10, null, null, queryContext);
    // TODO: parametrize dynamically
    return ok(reconciled);
  }

}
