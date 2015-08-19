package controllers;

import helpers.Countries;
import helpers.JSONForm;
import helpers.JsonLdConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Resource;

import org.json.simple.parser.ParseException;

import play.mvc.Result;
import play.mvc.Security;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import services.AggregationProvider;

/**
 * @author fo
 */
public class ResourceIndex extends OERWorldMap {

  public static Result list(String q, String sort, Boolean search) throws IOException, ParseException {

    Map<String, Object> scope = new HashMap<>();

    scope.put("q", q);
    scope.put("search", search);

    // Empty query string matches everything
    if (q.equals("")) {
      q = "*";
    }

    List<Resource> resources = mBaseRepository.esQuery(q, sort);
    scope.put("resources", resources);

    if (request().accepts("text/html")) {
      return ok(render("Resources", "ResourceIndex/index.mustache", scope));
    } else {
      return ok(resources.toString()).as("application/json");
    }
  }

  @Security.Authenticated(Secured.class)
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

    // FIXME: hardcoded access restriction to newsletter-only unsers, criteria:
    // has no unencrypted email address
    if (type.equals("Person") && null == resource.get("email")) {
      return notFound("Not found");
    }

    String title;
    try {
      title = ((Resource) ((ArrayList<?>) resource.get("name")).get(0)).get("@value").toString();
    } catch (NullPointerException e) {
      title = id;
    }

    if (request().accepts("text/html")) {
      return ok(render(title, "ResourceIndex/" + type + "/read.mustache", resource));
    } else {
      return ok(resource.toString()).as("application/json");
    }
  }

  /**
   * This method is designed to add information to existing resources. If the
   * resource doesn't exist yet, a bad request response is returned
   * 
   * @param id
   *          The ID of the resource to update
   * @throws IOException
   */
  @Security.Authenticated(Secured.class)
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

  @Security.Authenticated(Secured.class)
  public static Result delete(String id) {
    Resource resource = mBaseRepository.deleteResource(id);
    if (null != resource) {
      return ok("deleted resource " + resource.toString());
    } else {
      return badRequest("Failed to delete resource " + id);
    }
  }

}
