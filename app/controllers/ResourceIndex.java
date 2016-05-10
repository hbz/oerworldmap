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
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.ProcessingReport;

import helpers.JsonLdConstants;
import models.Commit;
import models.Resource;
import models.ResourceList;
import play.Logger;
import play.mvc.Result;
import services.AggregationProvider;
import services.QueryContext;
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

    // Check for bounding box
    String[] boundingBoxParam = request().queryString().get("boundingBox");
    if (boundingBoxParam != null && boundingBoxParam.length > 0) {
      String boundingBox = boundingBoxParam[0];
      if (boundingBox != null) {
        try {
          queryContext.setBoundingBox(boundingBox);
        } catch (NumberFormatException e) {
          Logger.error("Invalid bounding box: ".concat(boundingBox), e);
        }
      }
    }

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

  public static Result importRecords() throws IOException {
    JsonNode json = request().body().asJson();
    List<Resource> records = new ArrayList<>();
    if (json.isArray()) {
      for (JsonNode node : json) {
        records.add(Resource.fromJson(node));
      }
    } else if (json.isObject()) {
      records.add(Resource.fromJson(json));
    } else {
      return badRequest();
    }
    mBaseRepository.importRecords(records, getMetadata());
    return ok(Integer.toString(records.size()).concat(" records imported."));
  }

  public static Result importResources() throws IOException {
    JsonNode json = request().body().asJson();
    List<Resource> resources = new ArrayList<>();
    if (json.isArray()) {
      for (JsonNode node : json) {
        resources.add(Resource.fromJson(node));
      }
    } else if (json.isObject()) {
      resources.add(Resource.fromJson(json));
    } else {
      return badRequest();
    }
    mBaseRepository.importResources(resources, getMetadata());
    return ok(Integer.toString(resources.size()).concat(" resources imported."));
  }

  public static Result addResource() throws IOException {

    JsonNode jsonNode = getJsonFromRequest();

    if (jsonNode == null || (!jsonNode.isArray() && !jsonNode.isObject())) {
      return badRequest("Bad or empty JSON");
    } else if (jsonNode.isArray()) {
      return upsertResources();
    } else {
      return upsertResource(false);
    }

  }

  public static Result updateResource(String aId) throws IOException {

    // If updating a resource, check if it actually exists
    Resource originalResource = mBaseRepository.getResource(aId);
    if (originalResource == null) {
      return notFound("Not found: ".concat(aId));
    }

    return upsertResource(true);

  }

  private static Result upsertResource(boolean isUpdate) throws IOException {

    // Extract resource
    Resource resource = Resource.fromJson(getJsonFromRequest());
    resource.put(JsonLdConstants.CONTEXT, "http://schema.org/");

    // Person create /update only through UserIndex, which is restricted to admin
    if ("Person".equals(resource.getType())) {
      return forbidden("Upsert Person forbidden.");
    }

    // Validate
    ProcessingReport processingReport = mBaseRepository.stage(resource).validate();
    if (!processingReport.isSuccess()) {
      List<Map<String, Object>> messages = new ArrayList<>();
      HashMap<String, Object> message = new HashMap<>();
      message.put("level", "warning");
      message.put("message", OERWorldMap.messages.getString("schema_error")
        + "<pre>" + processingReport.toString() + "</pre>"
        + "<pre>" + resource + "</pre>");
      messages.add(message);
      return badRequest(render("Upsert failed", "feedback.mustache", null, messages));
    }

    // Save
    mBaseRepository.addResource(resource, getMetadata());

    // Respond
    if (isUpdate) {
      if (request().accepts("text/html")) {
        return ok(render("Updated", "updated.mustache", resource));
      } else {
        return ok("Updated " + resource.getId());
      }
    } else {
      response().setHeader(LOCATION, routes.ResourceIndex.read(resource.getId()).absoluteURL(request()));
      if (request().accepts("text/html")) {
        return created(render("Created", "created.mustache", resource));
      } else {
        return created("Created " + resource.getId());
      }
    }

  }

  private static Result upsertResources() throws IOException {

    // Extract resources
    List<Resource> resources = new ArrayList<>();
    for (JsonNode jsonNode : getJsonFromRequest()) {
      Resource resource = Resource.fromJson(jsonNode);
      resource.put(JsonLdConstants.CONTEXT, "http://schema.org/");
      resources.add(resource);
    }

    // Validate
    ListProcessingReport listProcessingReport = new ListProcessingReport();
    for (Resource resource : resources) {
      // Person create /update only through UserIndex, which is restricted to admin
      if ("Person".equals(resource.getType())) {
        return forbidden("Upsert Person forbidden.");
      }
      // Stage and validate each resource
      try {
        Resource staged = mBaseRepository.stage(resource);
        ProcessingReport processingMessages = staged.validate();
        if (!processingMessages.isSuccess()) {
          Logger.debug(processingMessages.toString());
          Logger.debug(staged.toString());
        }
        listProcessingReport.mergeWith(processingMessages);
      } catch (ProcessingException e) {
        Logger.error("Validation error", e);
        return badRequest();
      }
    }

    if (!listProcessingReport.isSuccess()) {
      List<Map<String, Object>> messages = new ArrayList<>();
      HashMap<String, Object> message = new HashMap<>();
      message.put("level", "warning");
      message.put("message", OERWorldMap.messages.getString("schema_error")
        + "<pre>" + listProcessingReport.toString() + "</pre>"
        + "<pre>" + resources + "</pre>");
      messages.add(message);
      return badRequest(render("Upsert failed", "feedback.mustache", null, messages));
    }

    mBaseRepository.addResources(resources, getMetadata());

    return ok("Added resources");

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

  public static Result export(String aId) {
    Resource record = mBaseRepository.getRecord(aId);
    if (null == record) {
      return notFound("Not found");
    }
    return ok(record.toString()).as("application/json");
  }

  public static Result delete(String id) throws IOException {
    Resource resource = mBaseRepository.deleteResource(id, getMetadata());
    if (null != resource) {
      return ok("deleted resource " + resource.toString());
    } else {
      return badRequest("Failed to delete resource " + id);
    }
  }

  public static Result log(String id) {

    StringBuilder stringBuilder = new StringBuilder();

    for (Commit commit : mBaseRepository.log(id)) {
      stringBuilder.append(commit).append("\n\n");
    }

    return ok(stringBuilder.toString());

  }

}
