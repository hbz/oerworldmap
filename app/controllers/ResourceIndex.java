package controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
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
import services.QueryContext;

import services.AggregationProvider;
import services.export.AbstractCsvExporter;
import services.export.CsvWithNestedIdsExporter;

/**
 * @author fo
 */
public class ResourceIndex extends OERWorldMap {

  public static Result list(String q, int from, int size, String sort, boolean list)
      throws IOException, ParseException {

    // Extract filters directly from query params
    Map<String, ArrayList<String>> filters = new HashMap<>();
    Pattern filterPattern = Pattern.compile("^filter\\.(.*)$");
    for (Map.Entry<String, String[]> entry : request().queryString().entrySet()) {
      Matcher filterMatcher = filterPattern.matcher(entry.getKey());
      if (filterMatcher.find()) {
        filters.put(filterMatcher.group(1), new ArrayList<>(Arrays.asList(entry.getValue())));
      }
    }

    Map<String, Object> scope = new HashMap<>();
    ResourceList resourceList = mBaseRepository.query(q, from, size, sort, filters,
        (QueryContext) ctx().args.get("queryContext"));
    scope.put("list", list);
    scope.put("resources", resourceList.toResource());

    if (request().accepts("text/html")) {
      return ok(render("Resources", "ResourceIndex/index.mustache", scope));
    } else if (request().accepts("text/csv")) {
      StringBuffer result = new StringBuffer();
      AbstractCsvExporter csvExporter = new CsvWithNestedIdsExporter();
      csvExporter.defineHeaderColumns(resourceList.getItems());
      List<String> dropFields = Arrays.asList(JsonLdConstants.TYPE);
      csvExporter.setDropFields(dropFields);
      result.append(csvExporter.headerKeysToCsvString().concat("\n"));
      for (Resource resource : resourceList.getItems()) {
        result.append(csvExporter.exportResourceAsCsvLine(resource).concat("\n"));
      }
      return ok(result.toString()).as("text/csv");
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
    String id = resource.getAsString(JsonLdConstants.ID);
    ProcessingReport report = mBaseRepository.validateAndAdd(resource);
    Map<String, Object> scope = new HashMap<>();
    scope.put("resource", resource);
    if (!report.isSuccess()) {
      scope.put("countries", Countries.list(currentLocale));
      if (isJsonRequest) {
        return badRequest("Failed to create " + id + "\n" + report.toString() + "\n");
      } else {
        return badRequest("Failed to create " + id + "\n" + report.toString() + "\n");
      }
    }
    return redirect(routes.ResourceIndex.read(id));
    /*
    response().setHeader(LOCATION, routes.ResourceIndex.create().absoluteURL(request()).concat(id));
    if (isJsonRequest) {
      return created("Created " + id + "\n");
    } else {
      return created(render("Created", "created.mustache", scope));
    }*/
  }

  public static Result read(String id) throws IOException {
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

    if (type.equals("Concept")) {
      ResourceList relatedList = mBaseRepository.query("about.about.@id:\"".concat(id)
          .concat("\" OR about.audience.@id:\"").concat(id).concat("\""), 0, 999, null, null);
      resource.put("related", relatedList.getItems());
    }

    if (type.equals("ConceptScheme")) {
      Resource conceptScheme = null;
      String field = null;
      if ("https://w3id.org/class/esc/scheme".equals(id)) {
        conceptScheme = Resource.fromJsonFile("public/json/esc.json");
        field = "about.about.@id";
      } else if ("https://w3id.org/isced/1997/scheme".equals(id)) {
        field = "about.audience.@id";
        conceptScheme = Resource.fromJsonFile("public/json/isced-1997.json");
      }
      if (!(null == conceptScheme)) {
        AggregationBuilder conceptAggregation = AggregationBuilders.filter("services")
            .filter(FilterBuilders.termFilter("about.@type", "Service"));
        for (Resource topLevelConcept : conceptScheme.getAsList("hasTopConcept")) {
          conceptAggregation.subAggregation(
              AggregationProvider.getNestedConceptAggregation(topLevelConcept, field));
        }
        Resource nestedConceptAggregation = mBaseRepository.aggregate(conceptAggregation);
        resource.put("aggregation", nestedConceptAggregation);
      }
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
        return badRequest("Failed to update " + id + "\n" + report.toString() + "\n");
      } else {
        return badRequest("Failed to update " + id + "\n" + report.toString() + "\n");
      }
    }
    return read(id);
    /*if (isJsonRequest) {
      return ok("Updated " + id + "\n");
    } else {
      return ok(render("Updated", "updated.mustache", scope));
    }*/
  }

  @Security.Authenticated(Secured.class)
  public static Result delete(String id) throws IOException {
    Resource resource = mBaseRepository.deleteResource(id);
    if (null != resource) {
      return ok("deleted resource " + resource.toString());
    } else {
      return badRequest("Failed to delete resource " + id);
    }
  }

}
