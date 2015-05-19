package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.mustachejava.MustacheNotFoundException;
import helpers.Countries;
import helpers.JSONForm;
import helpers.JsonLdConstants;
import models.Resource;
import org.json.simple.parser.ParseException;
import play.mvc.Result;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fo
 */
public class ResourceIndex extends OERWorldMap {

  public static Result list(String q) throws IOException, ParseException {
    // Empty query string matches everything
    if (q.equals("")) {
      q = "*";
    }
    // Only expose Articles for now
    q = "(" + q + ") AND (about.@type:Article)";
    List<Resource> resources = mBaseRepository.esQuery(q);
    Map<String, Object> scope = new HashMap<>();
    scope.put("resources", resources);

    if (request().accepts("text/html")) {
      return ok(render("Home", "ResourceIndex/index.mustache", scope));
    } else {
      return ok(resources.toString()).as("application/json");
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
    resource = mBaseRepository.getResource(id);
    if (null == resource) {
      return notFound("Not found");
    }
    String type = resource.get(JsonLdConstants.TYPE).toString();
    if (request().accepts("text/html")) {
      try {
        return ok(render("Home", "ResourceIndex/" + type + "/read.mustache", resource));
      } catch (MustacheNotFoundException ex) {
        return ok(render("Home", "ResourceIndex/read.mustache", resource));
      }
    } else {
      return ok(resource.toString()).as("application/json");
    }
  }

  /**
   * This method is designed to add information to existing resources. If the
   * resource doesn't exist yet, a bad request response is returned
   * 
   * @param id The ID of the resource to update
   * @throws IOException
   */
  public static Result update(String id) throws IOException {
    Resource originalResource = mBaseRepository.getResource(id);
    if (originalResource == null) {
      return badRequest("missing resource " + id);
    }

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
    return created("updated resource " + resource.toString());
  }

}
