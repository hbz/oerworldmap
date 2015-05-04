package controllers;

import helpers.Countries;
import helpers.JSONForm;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import models.Resource;

import org.json.simple.parser.ParseException;

import play.mvc.Result;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.fge.jsonschema.core.report.ProcessingReport;

/**
 * @author fo
 */
public class ResourceIndex extends OERWorldMap {

  public static Result list() throws IOException, ParseException {
    List<Resource> stories = mBaseRepository.query("Action", false);
    Map<String, Object> scope = new HashMap<>();
    scope.put("stories", stories);

    if (request().accepts("text/html")) {
      return ok(render("Home", "ResourceIndex/index.mustache", scope));
    } else {
      return ok(stories.toString()).as("application/json");
    }
  }

  public static Result create() throws IOException {
    boolean isJsonRequest = true;
    JsonNode json = request().body().asJson();
    if (null == json) {
      json = JSONForm.parseFormData(request().body().asFormUrlEncoded());
      isJsonRequest = false;
    }
    Resource resource = Resource.fromJson(json);
    ProcessingReport report = resource.validate();
    if (!report.isSuccess()) {
      Map<String, Object> scope = new HashMap<>();
      scope.put("resource", resource);
      scope.put("countries", Countries.list(currentLocale));
      if (isJsonRequest) {
        return badRequest(report.toString());
      } else {
        return badRequest(render("Resources", "ResourceIndex/index.mustache", scope,
            JSONForm.generateErrorReport(report)));
      }
    }
    mBaseRepository.addResource(resource);
    return created("created resource " + resource.toString());
  }

  public static Result read(String id) {
    Resource resource;
    try {
      resource = mBaseRepository.getResource(id);
    } catch (IOException ex) {
      return notFound("Not found");
    }
    return ok(render("Home", "ResourceIndex/read.mustache", resource));
  }

  /**
   * This method is designed to add information to existing resources. If the
   * resource doesn't exist yet, a bad request response is returned
   * 
   * @param id
   * @param json
   * @return
   * @throws IOException
   */
  public static Result put(String id, String json) throws IOException {
    Resource resource = mBaseRepository.getResource(id);
    if (resource == null) {
      return badRequest("missing resource " + id);
    }
    JsonNode jsonNode = JsonNodeFactory.instance.textNode(json);
    Iterator<Map.Entry<String, JsonNode>> it = jsonNode.fields();
    while (it.hasNext()) {
      Map.Entry<String, JsonNode> entry = (Map.Entry<String, JsonNode>) it.next();
      resource.put(entry.getKey(), entry.getValue());
    }
    mBaseRepository.addResource(resource);
    return created("created resource " + resource.toString());
  }

}