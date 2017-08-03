package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import models.Resource;
import org.apache.commons.lang3.StringUtils;
import play.Configuration;
import play.Environment;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.libs.Json;
import play.mvc.Result;
import services.QueryContext;

import java.util.Iterator;
import java.util.Map;

/**
 * @author pvb
 */
public class Reconciler extends OERWorldMap{

  @Inject
  FormFactory formFactory;

  @Inject
  public Reconciler(Configuration aConf, Environment aEnv) {
    super(aConf, aEnv);
  }

  public Result meta(String aCallback) {
    ObjectNode result = Json.newObject();
    result.put("name", "oerworldmap reconciliation");
    result.put("identifierSpace", "https://oerworldmap.org/resource");
    result.put("schemaSpace", "https://oerworldmap.org/resource");
    ArrayNode defaultTypes = Json.newArray();
    Resource.mIdentifiedTypes.forEach(x -> {
      ObjectNode defaultType = Json.newObject();
      defaultType.put("id", x);
      defaultType.put("name", x);
      defaultTypes.add(defaultType);
    });
    result.set("defaultTypes", defaultTypes);
    result.set("view", Json.newObject().put("url", "https://oerworldmap.org/resource/{{id}}"));
    return StringUtils.isEmpty(aCallback) ? ok(result)
      : ok(String.format("/**/%s(%s);", aCallback, result.toString()));
  }

  public Result  reconcile() {
    DynamicForm requestData = formFactory.form().bindFromRequest();
    JsonNode request = Json.parse(requestData.get("queries"));

    System.out.println(request); // TODO: remove (?)

    Iterator<Map.Entry<String, JsonNode>> inputQueries = request.fields();
    return ok(reconcile(inputQueries, null));
  }

  public JsonNode reconcile(final Iterator<Map.Entry<String, JsonNode>> aInputQueries, final QueryContext aQueryContext) {

    QueryContext queryContext = aQueryContext != null ? aQueryContext : getQueryContext();

    ObjectNode response = Json.newObject();
    while (aInputQueries.hasNext()) {
      Map.Entry<String, JsonNode> inputQuery = aInputQueries.next();

      System.out.println("Query entry: " + inputQuery);

      JsonNode limitNode = inputQuery.getValue().get("limit");
      int limit = limitNode == null ? -1 : limitNode.asInt();
      String queryString = inputQuery.getValue().get("query").asText();
      JsonNode reconciled = mBaseRepository.reconcile(queryString, 0, limit, null, null, queryContext);
      response.set(inputQuery.getKey(), reconciled);
    }

    System.out.println(response); // TODO: remove (?)
    return response;
  }

}
