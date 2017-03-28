package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import helpers.JSONForm;
import helpers.JsonLdConstants;
import helpers.SCHEMA;
import models.Commit;
import models.Record;
import models.Resource;
import models.ResourceList;
import models.TripleCommit;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.ResourceFactory;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import play.Configuration;
import play.Environment;
import play.Logger;
import play.mvc.Result;
import services.AggregationProvider;
import services.QueryContext;
import services.SearchConfig;
import services.export.CalendarExporter;
import services.export.CsvWithNestedIdsExporter;

import javax.inject.Inject;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author fo
 */
public class ResourceIndex extends OERWorldMap {

  @Inject
  public ResourceIndex(Configuration aConf, Environment aEnv) {
    super(aConf, aEnv);
  }

  public Result listDefault(String q, int from, int size, String sort, boolean list) throws IOException {
    return list(q, from, size, sort, list, null);
  }

  public Result list(String q, int from, int size, String sort, boolean list, String extension)
      throws IOException {

    // Extract filters directly from query params
    Map<String, List<String>> filters = new HashMap<>();
    Pattern filterPattern = Pattern.compile("^filter\\.(.*)$");
    for (Map.Entry<String, String[]> entry : ctx().request().queryString().entrySet()) {
      Matcher filterMatcher = filterPattern.matcher(entry.getKey());
      if (filterMatcher.find()) {
        filters.put(filterMatcher.group(1), new ArrayList<>(Arrays.asList(entry.getValue())));
      }
    }

    QueryContext queryContext = getQueryContext();

    // Check for bounding box
    String[] boundingBoxParam = ctx().request().queryString().get("boundingBox");
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

    // Sort by dateCreated if no query string given
    if (StringUtils.isEmpty(q) && StringUtils.isEmpty(sort)) {
      sort = "dateCreated:DESC";
    }

    queryContext.setFetchSource(new String[]{
      "@id", "@type", "dateCreated", "author", "dateModified", "contributor",
      "about.@id", "about.@type", "about.name", "about.alternateName", "about.location", "about.image",
      "about.provider.@id", "about.provider.@type", "about.provider.name", "about.provider.location",
      "about.participant.@id", "about.participant.@type", "about.participant.name", "about.participant.location",
      "about.agent.@id", "about.agent.@type", "about.agent.name", "about.agent.location",
      "about.mentions.@id", "about.mentions.@type", "about.mentions.name", "about.mentions.location",
      "about.mainEntity.@id", "about.mainEntity.@type", "about.mainEntity.name", "about.mainEntity.location",
      "about.startDate", "about.endDate", "about.organizer", "about.description"
    });

    queryContext.setElasticsearchFieldBoosts(new SearchConfig().getBoostsForElasticsearch());

    ResourceList resourceList = mBaseRepository.query(q, from, size, sort, filters, queryContext);

    Map<String, String> alternates = new HashMap<>();
    String baseUrl = mConf.getString("proxy.host");
    String filterString = "";
    for (Map.Entry<String, List<String>> filter : filters.entrySet()) {
      String filterKey = "filter.".concat(filter.getKey());
      for (String filterValue : filter.getValue()) {
        filterString = filterString.concat("&".concat(filterKey).concat("=").concat(filterValue));
      }
    }

    alternates.put("JSON", baseUrl.concat(routes.ResourceIndex.list(q, from, size, sort, list, "json").url().concat(filterString)));
    alternates.put("CSV", baseUrl.concat(routes.ResourceIndex.list(q, from, size, sort, list, "csv").url().concat(filterString)));
    if (resourceList.containsType("Event")) {
      alternates.put("iCal", baseUrl.concat(routes.ResourceIndex.list(q, from, size, sort, list, "ics").url().concat(filterString)));
    }

    Map<String, Object> scope = new HashMap<>();
    scope.put("list", list);
    scope.put("resources", resourceList.toResource());
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
      return ok(render("OER World Map", "ResourceIndex/index.mustache", scope));
    } //
    else if (format.equals("text/csv")) {
      return ok(new CsvWithNestedIdsExporter().export(resourceList)).as("text/csv");
    } //
    else if (format.equals("text/calendar")) {
      return ok(new CalendarExporter(Locale.ENGLISH).export(resourceList)).as("text/calendar");
    } //
    else if (format.equals("application/json")) {
      return ok(resourceList.toResource().toString()).as("application/json");
    }

    return notFound("Not found");

  }

  public Result importResources() throws IOException {
    JsonNode json = ctx().request().body().asJson();
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

  public Result addResource() throws IOException {

    JsonNode jsonNode = getJsonFromRequest();

    if (jsonNode == null || (!jsonNode.isArray() && !jsonNode.isObject())) {
      return badRequest("Bad or empty JSON");
    } else if (jsonNode.isArray()) {
      return upsertResources();
    } else {
      return upsertResource(false);
    }

  }

  public Result updateResource(String aId) throws IOException {

    // If updating a resource, check if it actually exists
    Resource originalResource = mBaseRepository.getResource(aId);
    if (originalResource == null) {
      return notFound("Not found: ".concat(aId));
    }

    return upsertResource(true);

  }

  private Result upsertResource(boolean isUpdate) throws IOException {

    // Extract resource
    Resource resource = Resource.fromJson(getJsonFromRequest());
    resource.put(JsonLdConstants.CONTEXT, mConf.getString("jsonld.context"));

    // Person create /update only through UserIndex, which is restricted to admin
    if (!isUpdate && "Person".equals(resource.getType())) {
      return forbidden("Upsert Person forbidden.");
    }

    // Validate
    Resource staged = mBaseRepository.stage(resource);
    ProcessingReport processingReport = staged.validate();
    if (!processingReport.isSuccess()) {
      List<Map<String, Object>> messages = new ArrayList<>();
      HashMap<String, Object> message = new HashMap<>();
      message.put("level", "warning");
      message.put("message", getMessages().getString("schema_error")
        + "<pre>" + processingReport.toString() + "</pre>"
        + "<pre>" + staged + "</pre>");
      messages.add(message);
      return badRequest(render("Upsert failed", "feedback.mustache", null, messages));
    }

    // Save
    mBaseRepository.addResource(resource, getMetadata());

    // Respond
    if (isUpdate) {
      if (request().accepts("text/html")) {
        return read(resource.getId(), "HEAD", "html");
      } else {
        return ok("Updated " + resource.getId());
      }
    } else {
      response().setHeader(LOCATION, routes.ResourceIndex.readDefault(resource.getId(), "HEAD")
        .absoluteURL(request()));
      if (request().accepts("text/html")) {
        return created(render("Created", "created.mustache", resource));
      } else {
        return created("Created " + resource.getId());
      }
    }

  }

  private Result upsertResources() throws IOException {

    // Extract resources
    List<Resource> resources = new ArrayList<>();
    for (JsonNode jsonNode : getJsonFromRequest()) {
      Resource resource = Resource.fromJson(jsonNode);
      resource.put(JsonLdConstants.CONTEXT, mConf.getString("jsonld.context"));
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
      message.put("message", getMessages().getString("schema_error")
        + "<pre>" + listProcessingReport.toString() + "</pre>"
        + "<pre>" + resources + "</pre>");
      messages.add(message);
      return badRequest(render("Upsert failed", "feedback.mustache", null, messages));
    }

    mBaseRepository.addResources(resources, getMetadata());

    return ok("Added resources");

  }

  public Result readDefault(String id, String version) throws IOException {
    return read(id, version, null);
  }


  public Result read(String id, String version, String extension) throws IOException {
    Resource resource;
    resource = mBaseRepository.getResource(id, version);
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
        conceptScheme = Resource.fromJson(mEnv.classLoader().getResourceAsStream("public/json/esc.json"));
        field = "about.about.@id";
      } else if ("https://w3id.org/isced/1997/scheme".equals(id)) {
        field = "about.audience.@id";
        conceptScheme = Resource.fromJson(mEnv.classLoader().getResourceAsStream("public/json/isced-1997.json"));
      }
      if (!(null == conceptScheme)) {
        AggregationBuilder conceptAggregation = AggregationBuilders.filter("services")
            .filter(QueryBuilders.termQuery("about.@type", "Service"));
        for (Resource topLevelConcept : conceptScheme.getAsList("hasTopConcept")) {
          conceptAggregation.subAggregation(
              AggregationProvider.getNestedConceptAggregation(topLevelConcept, field));
        }
        Resource nestedConceptAggregation = mBaseRepository.aggregate(conceptAggregation);
        resource.put("aggregation", nestedConceptAggregation);
        return ok(render("", "ResourceIndex/ConceptScheme/read.mustache", resource));
      }
    }

    List<Resource> comments = new ArrayList<>();
    for (String commentId : resource.getIdList("comment")) {
      comments.add(mBaseRepository.getResource(commentId));
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
      Logger.error("Could not read author from commit " + history.get(history.size() - 1), e);
    }
    resource.put(Record.DATE_MODIFIED, history.get(0).getHeader().getTimestamp()
      .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    try {
      resource.put(Record.DATE_CREATED, history.get(history.size() - 1).getHeader().getTimestamp()
        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    } catch (NullPointerException e) {
      Logger.error("Could not read timestamp from commit " + history.get(history.size() - 1), e);
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

  public Result delete(String aId) throws IOException {
    Resource resource = mBaseRepository.deleteResource(aId, getMetadata());
    if (null != resource) {
      // If deleting personal profile, also delete corresponding user
      if ("Person".equals(resource.getType())) {
        String username = mAccountService.getUsername(aId);
        if (!mAccountService.removePermissions(aId)) {
          Logger.warn("Could not remove permissions for " + aId);
        }
        if (!mAccountService.setProfileId(username, null)) {
          Logger.warn("Could not unset profile ID for " + username);
        }
        if (!mAccountService.deleteUser(username)) {
          Logger.warn("Could not delete user " + username);
        }
        return ok("deleted user " + aId);
      } else {
        return ok("deleted resource " + aId);
      }
    } else {
      return badRequest("Failed to delete resource " + aId);
    }
  }

  public Result log(String aId) {

    Map<String, Object> scope = new HashMap<>();
    scope.put("commits", mBaseRepository.log(aId));
    scope.put("resource", aId);

    if (StringUtils.isEmpty(aId)) {
      return ok(mBaseRepository.log(aId).toString());
    }
    return ok(render("Log ".concat(aId), "ResourceIndex/log.mustache", scope));

  }

  public Result index(String aId) {
    mBaseRepository.index(aId);
    return ok("Indexed ".concat(aId));
  }

  public Result commentResource(String aId) throws IOException {

    ObjectNode jsonNode = (ObjectNode) JSONForm.parseFormData(request().body().asFormUrlEncoded());
    jsonNode.put(JsonLdConstants.CONTEXT, mConf.getString("jsonld.context"));
    Resource comment = Resource.fromJson(jsonNode);

    comment.put("author", getUser());
    comment.put("dateCreated", ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

    TripleCommit.Diff diff = (TripleCommit.Diff) mBaseRepository.getDiff(comment);
    diff.addStatement(ResourceFactory.createStatement(
      ResourceFactory.createResource(aId), SCHEMA.comment, ResourceFactory.createResource(comment.getId())
    ));

    Map<String, String> metadata = getMetadata();
    TripleCommit.Header header = new TripleCommit.Header(metadata.get(TripleCommit.Header.AUTHOR_HEADER),
      ZonedDateTime.parse(metadata.get(TripleCommit.Header.DATE_HEADER)));
    TripleCommit commit = new TripleCommit(header, diff);
    mBaseRepository.commit(commit);

    return seeOther("/resource/" + aId);

  }

  public Result feed() {

    ResourceList resourceList = mBaseRepository.query("", 0, 20, "dateCreated:DESC", null, getQueryContext());
    Map<String, Object> scope = new HashMap<>();
    scope.put("resources", resourceList.toResource());

    return ok(render("OER World Map", "ResourceIndex/feed.mustache", scope));

  }

  public Result label(String aId) {

    return ok(mBaseRepository.label(aId));

  }

}
