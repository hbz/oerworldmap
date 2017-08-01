package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import models.Resource;
import org.apache.commons.lang3.StringUtils;
import play.Configuration;
import play.Environment;
import play.libs.Json;
import play.mvc.Result;
import services.QueryContext;

/**
 * @author pvb
 */
public class Reconciler extends OERWorldMap{

  @Inject
  public Reconciler(Configuration aConf, Environment aEnv) {
    super(aConf, aEnv);
  }


  public Result reconcile(String aQueryTerm) {

    if (StringUtils.isEmpty(aQueryTerm)) {
      ObjectNode result = Json.newObject();
      result.put("name", "oerworldmap reconciliation");
      result.put("identifierSpace", "https://oerworldmap.org/resource");
      result.put("schemaSpace", "https://oerworldmap.org/resource");
      result.set("defaultTypes", Json.toJson(Resource.mIdentifiedTypes));
      result.set("view", Json.newObject().put("url", "https://oerworldmap.org/resource/{{id}}"));
      return ok(result);
    } else {
      QueryContext queryContext = getQueryContext();
      final JsonNode reconciled = mBaseRepository.reconcile(aQueryTerm, 0, 10, null, null, queryContext);
      // TODO: parametrize dynamically
      return ok(reconciled);
    }
  }

}
