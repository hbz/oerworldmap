package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import helpers.JsonLdConstants;
import models.*;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import play.Configuration;
import play.Environment;
import play.Logger;
import play.mvc.Result;
import services.AggregationProvider;
import services.export.CalendarExporter;
import services.export.CsvWithNestedIdsExporter;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author pvb
 */

public abstract class IndexCommon extends OERWorldMap{

  public IndexCommon(Configuration aConf, Environment aEnv) {
    super(aConf, aEnv);
  }
  private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public Result addItem() throws IOException {
    JsonNode jsonNode = getJsonFromRequest();
    if (jsonNode == null || (!jsonNode.isArray() && !jsonNode.isObject())) {
      return badRequest("Bad or empty JSON");
    } else if (jsonNode.isArray()) {
      return upsertItems();
    } else {
      return upsertItem(false);
    }
  }

  protected abstract Result upsertItem(boolean isUpdate) throws IOException;

  protected Result upsertItem(boolean isUpdate, final ReverseResourceIndex aResourceIndex,
                                  final String... aForbiddenTypes) throws IOException {
    // Extract resource
    Resource resource = new Resource(getJsonFromRequest());
    resource.put(JsonLdConstants.CONTEXT, mConf.getString("jsonld.context"));

    // Person create /update only through UserIndex, which is restricted to admin
    for (String forbiddenType : aForbiddenTypes) {
      if (!isUpdate && forbiddenType.equals(resource.getType())) {
        return forbidden(String.format("Upsert %s forbidden.", forbiddenType));
      }
    }

    // Validate
    Resource staged = mBaseRepository.stage(resource);
    ProcessingReport processingReport = staged.validate();
    if (!processingReport.isSuccess()) {
      ListProcessingReport listProcessingReport = new ListProcessingReport();
      try {
        listProcessingReport.mergeWith(processingReport);
      } catch (ProcessingException e) {
        Logger.warn("Failed to create list processing report", e);
      }
      if (request().accepts("text/html")) {
        Map<String, Object> scope = new HashMap<>();
        scope.put("report", OBJECT_MAPPER.convertValue(listProcessingReport.asJson(), ArrayList.class));
        scope.put("type", resource.getType());
        return badRequest(render("Upsert failed", "ProcessingReport/list.mustache", scope));
      } else {
        return badRequest(listProcessingReport.asJson());
      }
    }

    // Save
    mBaseRepository.addItem(resource, getMetadata());

    // Respond
    if (isUpdate) {
      if (request().accepts("text/html")) {
        return read(resource.getId(), "HEAD", "html");
      } else {
        return ok("Updated " + resource.getId());
      }
    } else {
      response().setHeader(LOCATION, aResourceIndex.readDefault(resource.getId(), "HEAD")
        .absoluteURL(request()));
      if (request().accepts("text/html")) {
        return created(render("Created", "created.mustache", resource));
      } else {
        return created("Created " + resource.getId());
      }
    }
  }

  protected abstract Result upsertItems() throws IOException;

  protected Result upsertItems(final String... aForbiddenTypes) throws IOException {

    // Extract resources
    List<Resource> resources = new ArrayList<>();
    for (JsonNode jsonNode : getJsonFromRequest()) {
      Resource resource = new Resource(jsonNode);
      resource.put(JsonLdConstants.CONTEXT, mConf.getString("jsonld.context"));
      resources.add(resource);
    }

    // Validate
    ListProcessingReport listProcessingReport = new ListProcessingReport();
    for (Resource resource : resources) {
      // Person create /update only through UserIndex, which is restricted to admin
      for (String forbiddenType : aForbiddenTypes) {
        if (forbiddenType.equals(resource.getType())) {
          return forbidden(String.format("Upsert %s forbidden.", forbiddenType));
        }
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
        Logger.error("Could not process validation report", e);
        return badRequest();
      }
    }

    if (!listProcessingReport.isSuccess()) {
      return badRequest(listProcessingReport.asJson());
    }
    mBaseRepository.addItems(resources, getMetadata());
    return ok("Added resources");
  }

  public Result read(String id, String version, String extension) throws IOException {
    Resource resource = mBaseRepository.getItem(id, version);
    if (null == resource) {
      return notFound("Not found");
    }
    String type = resource.get(JsonLdConstants.TYPE).toString();
    String[] indices = new String[]{mConf.getString("es.index.webpage.name")};
    if (type.equals("Concept")) {
      ResourceList relatedList = mBaseRepository.query("about.about.@id:\"".concat(id)
        .concat("\" OR about.audience.@id:\"").concat(id).concat("\""), 0, 999, null, null, indices);
      resource.put("related", relatedList.getItems());
    }

    if (type.equals("ConceptScheme")) {
      Resource conceptScheme = null;
      String field = null;
      if ("https://w3id.org/class/esc/scheme".equals(id)) {
        conceptScheme = new Resource(mEnv.classLoader().getResourceAsStream("public/json/esc.json"));
        field = "about.about.@id";
      } else if ("https://w3id.org/isced/1997/scheme".equals(id)) {
        field = "about.audience.@id";
        conceptScheme = new Resource(mEnv.classLoader().getResourceAsStream("public/json/isced-1997.json"));
      }
      if (!(null == conceptScheme)) {
        AggregationBuilder conceptAggregation = AggregationBuilders.filter("services")
          .filter(QueryBuilders.termQuery("about.@type", "Service"));
        for (ModelCommon topLevelConcept : conceptScheme.getAsList("hasTopConcept")) {
          conceptAggregation.subAggregation(
            AggregationProvider.getNestedConceptAggregation(topLevelConcept, field));
        }
        Resource nestedConceptAggregation = mBaseRepository.aggregate(conceptAggregation, indices);
        resource.put("aggregation", nestedConceptAggregation);
        return ok(render("", "ResourceIndex/ConceptScheme/read.mustache", resource));
      }
    }

    List<Resource> comments = new ArrayList<>();
    for (String commentId : resource.getIdList("comment")) {
      comments.add(mBaseRepository.getItem(commentId));
    }

    String title;
    try {
      title = ((Resource) ((ArrayList<?>) resource.get("name")).get(0)).get("@value").toString();
    } catch (NullPointerException e) {
      title = id;
    }

    boolean mayEdit = (getUser() != null) && ((resource.getType().equals("Person") && getUser().getId().equals(id))
      || (!resource.getType().equals("Person"))
      || mAccountService.getGroups(getHttpBasicAuthUser()).contains("admin"));
    boolean mayLog = (getUser() != null) && (mAccountService.getGroups(getHttpBasicAuthUser()).contains("admin")
      || mAccountService.getGroups(getHttpBasicAuthUser()).contains("editor"));
    boolean mayAdminister = (getUser() != null) && mAccountService.getGroups(getHttpBasicAuthUser()).contains("admin");
    boolean mayComment = (getUser() != null) && (!resource.getType().equals("Person"));
    boolean mayDelete = (getUser() != null) && (resource.getType().equals("Person") && getUser().getId().equals(id)
      || mAccountService.getGroups(getHttpBasicAuthUser()).contains("admin"));

    Map<String, Object> permissions = new HashMap<>();
    permissions.put("edit", mayEdit);
    permissions.put("log", mayLog);
    permissions.put("administer", mayAdminister);
    permissions.put("comment", mayComment);
    permissions.put("delete", mayDelete);

    Map<String, String> alternates = new HashMap<>();
    String baseUrl = mConf.getString("proxy.host");
    alternates.put("JSON", baseUrl.concat(routes.ResourceIndex.read(id, version, "json").url()));
    alternates.put("CSV", baseUrl.concat(routes.ResourceIndex.read(id, version, "csv").url()));
    if (resource.getType().equals("Event")) {
      alternates.put("iCal", baseUrl.concat(routes.ResourceIndex.read(id, version, "ics").url()));
    }

    List<Commit> history = mBaseRepository.log(id);
    resource = new Record(resource);
    resource.put(Record.CONTRIBUTOR, history.get(0).getHeader().getAuthor());
    try {
      resource.put(Record.AUTHOR, history.get(history.size() - 1).getHeader().getAuthor());
    } catch (NullPointerException e) {
      Logger.trace("Could not read author from commit " + history.get(history.size() - 1), e);
    }
    resource.put(Record.DATE_MODIFIED, history.get(0).getHeader().getTimestamp()
      .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    try {
      resource.put(Record.DATE_CREATED, history.get(history.size() - 1).getHeader().getTimestamp()
        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    } catch (NullPointerException e) {
      Logger.trace("Could not read timestamp from commit " + history.get(history.size() - 1), e);
    }

    Map<String, Object> scope = new HashMap<>();
    scope.put("resource", resource);
    scope.put("comments", comments);
    scope.put("permissions", permissions);
    scope.put("alternates", alternates);

    String format = null;
    if (! StringUtils.isEmpty(extension)) {
      switch (extension) {
        case "html":
          format = "text/html";
          break;
        case "json":
          format = "application/json";
          break;
        case "csv":
          format = "text/csv";
          break;
        case "ics":
          format = "text/calendar";
          break;
      }
    } else if (request().accepts("text/html")) {
      format = "text/html";
    } else if (request().accepts("text/csv")) {
      format = "text/csv";
    } else if (request().accepts("text/calendar")) {
      format = "text/calendar";
    } else {
      format = "application/json";
    }

    if (format == null) {
      return notFound("Not found");
    } else if (format.equals("text/html")) {
      return ok(render(title, "ResourceIndex/read.mustache", scope));
    } else if (format.equals("application/json")) {
      return ok(resource.toString()).as("application/json");
    } else if (format.equals("text/csv")) {
      return ok(new CsvWithNestedIdsExporter().export(resource)).as("text/csv");
    } else if (format.equals("text/calendar")) {
      String ical = new CalendarExporter(Locale.ENGLISH).export(resource);
      if (ical != null) {
        return ok(ical).as("text/calendar");
      }
    }
    return notFound("Not found");
  }

  protected Result importResources() throws IOException {
    JsonNode json = ctx().request().body().asJson();
    List<Resource> resources = new ArrayList<>();
    if (json.isArray()) {
      for (JsonNode node : json) {
        resources.add(new Resource(node));
      }
    } else if (json.isObject()) {
      resources.add(new Resource(json));
    } else {
      return badRequest();
    }
    mBaseRepository.importItems(resources, getMetadata());
    return ok(Integer.toString(resources.size()).concat(" resources imported."));
  }


  protected Result updateItem(String aId) throws IOException {
    // If updating a resource, check if it actually exists
    Resource originalResource = mBaseRepository.getItem(aId);
    if (originalResource == null) {
      return notFound("Not found: ".concat(aId));
    }
    return upsertItem(true);
  }


  public Result label(String aId) {
    return ok(mBaseRepository.label(aId));
  }
}
