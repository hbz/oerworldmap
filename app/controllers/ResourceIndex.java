package controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.parser.ParseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.report.ProcessingReport;

import helpers.Countries;
import helpers.JSONForm;
import helpers.JsonLdConstants;
import models.Resource;
import models.ResourceList;
import play.mvc.Result;
import play.mvc.Security;
import services.ElasticsearchProvider;

/**
 * @author fo
 */
public class ResourceIndex extends OERWorldMap {

  public static Result list(String q, int from, int size, String sort)
      throws IOException, ParseException {

    // Extract filters directly from query params
    Map<String, String[]> filters = new HashMap<>();
    Pattern filterPattern = Pattern.compile("^filter\\.(.*)$");
    for (Map.Entry<String, String[]> entry : request().queryString().entrySet()) {
      Matcher filterMatcher = filterPattern.matcher(entry.getKey());
      if (filterMatcher.find()) {
        filters.put(filterMatcher.group(1), entry.getValue());
      }
    }

    Map<String, Object> scope = new HashMap<>();
    ElasticsearchProvider.user = Secured.getHttpBasicAuthUser(ctx());
    ResourceList resourceList = mBaseRepository.query(q, from, size, sort, filters);
    scope.put("resources", resourceList.toResource());

    if (request().accepts("text/html")) {
      return ok(render("Resources", "ResourceIndex/index.mustache", scope));
    } else {
      return ok(resourceList.toResource().toString()).as("application/json");
    }
  }

  @Security.Authenticated(Secured.class)
  public static Result create() throws IOException {
    boolean isJsonRequest = true;
    JsonNode json = request().body().asJson();
    if (null == json) {
      json = JSONForm.parseFormData(request().body().asFormUrlEncoded(), true);
      isJsonRequest = false;
    }
    Resource resource = Resource.fromJson(json);
    ProcessingReport report = mBaseRepository.validateAndAdd(resource);
    Map<String, Object> scope = new HashMap<>();
    scope.put("resource", resource);
    if (!report.isSuccess()) {
      scope.put("countries", Countries.list(currentLocale));
      if (isJsonRequest) {
        return badRequest(resource + report.toString());
      } else {
        return badRequest(resource + report.toString());
      }
    }
    response().setHeader(LOCATION, routes.ResourceIndex.create().absoluteURL(request())
        .concat(resource.getAsString(JsonLdConstants.ID)));
    return created(render("Created", "created.mustache", scope));
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
      json = JSONForm.parseFormData(request().body().asFormUrlEncoded(), true);
      isJsonRequest = false;
    }
    Resource resource = Resource.fromJson(json);
    ProcessingReport report = mBaseRepository.validateAndAdd(resource);
    Map<String, Object> scope = new HashMap<>();
    scope.put("resource", resource);
    if (!report.isSuccess()) {
      scope.put("countries", Countries.list(currentLocale));
      if (isJsonRequest) {
        return badRequest(resource + report.toString());
      } else {
        return badRequest(resource + report.toString());
      }
    }
    return ok(render("Updated", "updated.mustache", scope));
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
