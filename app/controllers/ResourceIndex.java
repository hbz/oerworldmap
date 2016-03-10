package controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
import services.QueryContext;

import services.AggregationProvider;
import services.SearchConfig;
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

    QueryContext queryContext = (QueryContext) ctx().args.get("queryContext");

    queryContext.setFetchSource(new String[]{
      "about.@id", "about.@type", "about.name", "about.alternateName", "about.location", "about.image",
      "about.provider.@id", "about.provider.@type", "about.provider.name", "about.provider.location",
      "about.participant.@id", "about.participant.@type", "about.participant.name", "about.participant.location",
      "about.agent.@id", "about.agent.@type", "about.agent.name", "about.agent.location",
      "about.mentions.@id", "about.mentions.@type", "about.mentions.name", "about.mentions.location",
      "about.mainEntity.@id", "about.mainEntity.@type", "about.mainEntity.name", "about.mainEntity.location"
    });

    queryContext.setElasticsearchFieldBoosts(new SearchConfig().getBoostsForElasticsearch());

    Map<String, Object> scope = new HashMap<>();
    ResourceList resourceList = mBaseRepository.query(q, from, size, sort, filters, queryContext);
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

  public static Result create() throws IOException {
    boolean isJsonRequest = true;
    JsonNode json = request().body().asJson();
    if (null == json) {
      Map<String, String[]> formUrlEncoded = request().body().asFormUrlEncoded();
      if (null == formUrlEncoded) {
        return badRequest("Empty request body");
      }
      json = JSONForm.parseFormData(formUrlEncoded, true);
      isJsonRequest = false;
    }
    Resource resource = Resource.fromJson(json);

    // Person create through UserIndex, which is restricted to admin
    if ("Person".equals(resource.getAsString(JsonLdConstants.TYPE))) {
      List<Map<String, Object>> messages = new ArrayList<>();
      HashMap<String, Object> message = new HashMap<>();
      message.put("level", "warning");
      message.put("message", "Forbidden");
      messages.add(message);
      return forbidden(render("Update failed", "feedback.mustache", resource, messages));
    }

    String id = resource.getAsString(JsonLdConstants.ID);
    ProcessingReport report = mBaseRepository.validateAndAdd(resource);
    Map<String, Object> scope = new HashMap<>();
    scope.put("resource", resource);
    if (!report.isSuccess()) {
      scope.put("countries", Countries.list(Locale.getDefault()));
      if (isJsonRequest) {
        return badRequest("Failed to create " + id + "\n" + report.toString() + "\n");
      } else {
        List<Map<String, Object>> messages = new ArrayList<>();
        HashMap<String, Object> message = new HashMap<>();
        message.put("level", "warning");
        message.put("message", OERWorldMap.messages.getString("schema_error")  + "<pre>" + report.toString() + "</pre>"
          + "<pre>" + resource + "</pre>");
        messages.add(message);
        return badRequest(render("Create failed", "feedback.mustache", scope, messages));
      }
    }
    response().setHeader(LOCATION, routes.ResourceIndex.create().absoluteURL(request()).concat(id));
    if (isJsonRequest) {
      return created("Created " + id + "\n");
    } else {
      return created(render("Created", "created.mustache", scope));
    }
  }

  public static Result read(String id) throws IOException {
    Resource resource;
    resource = mBaseRepository.getResource(id);
    if (null == resource) {
      return notFound("Not found");
    }
    String type = resource.get(JsonLdConstants.TYPE).toString();

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

    // Person update through UserIndex, which is restricted to owner
    if ("Person".equals(resource.getAsString(JsonLdConstants.TYPE))) {
      List<Map<String, Object>> messages = new ArrayList<>();
      HashMap<String, Object> message = new HashMap<>();
      message.put("level", "warning");
      message.put("message", "Forbidden");
      messages.add(message);
      return forbidden(render("Update failed", "feedback.mustache", resource, messages));
    }

    ProcessingReport report = mBaseRepository.validateAndAdd(resource);
    Map<String, Object> scope = new HashMap<>();
    scope.put("resource", resource);
    if (!report.isSuccess()) {
      scope.put("countries", Countries.list(Locale.getDefault()));
      if (isJsonRequest) {
        return badRequest("Failed to update " + id + "\n" + report.toString() + "\n");
      } else {
        List<Map<String, Object>> messages = new ArrayList<>();
        HashMap<String, Object> message = new HashMap<>();
        message.put("level", "warning");
        message.put("message", OERWorldMap.messages.getString("schema_error")  + "<pre>" + report.toString() + "</pre>"
          + "<pre>" + resource + "</pre>");
        messages.add(message);
        return badRequest(render("Update failed", "feedback.mustache", scope, messages));
      }
    }
    if (isJsonRequest) {
      return ok("Updated " + id + "\n");
    } else {
      return ok(render("Updated", "updated.mustache", scope));
    }
  }

  public static Result delete(String id) throws IOException {
    Resource resource = mBaseRepository.deleteResource(id);
    if (null != resource) {
      return ok("deleted resource " + resource.toString());
    } else {
      return badRequest("Failed to delete resource " + id);
    }
  }

}
