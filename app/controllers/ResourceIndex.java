package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import helpers.JSONForm;
import helpers.JsonLdConstants;
import helpers.MimeTypes;
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
import services.export.GeoJsonExporter;
import services.export.JsonSchemaExporter;

import javax.inject.Inject;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author fo
 */
public class ResourceIndex extends OERWorldMap {

  private GeoJsonExporter mGeoJsonExporter = new GeoJsonExporter();

  @Inject
  public ResourceIndex(Configuration aConf, Environment aEnv) {
    super(aConf, aEnv);
  }

  public Result list(String q, int from, int size, String sort, boolean list, String extension, String iso3166)
      throws IOException {

    Map<String, List<String>> filters = new HashMap<>();
    QueryContext queryContext = getQueryContext();

    // Handle ISO 3166 param
    if (!StringUtils.isEmpty(iso3166)) {

      if (! Arrays.asList(Locale.getISOCountries()).contains(iso3166.toUpperCase())) {
        return notFound("Not found");
      }

      queryContext.setIso3166Scope(iso3166.toUpperCase());

    }

    // Extract filters directly from query params
    Pattern filterPattern = Pattern.compile("^filter\\.(.*)$");
    for (Map.Entry<String, String[]> entry : ctx().request().queryString().entrySet()) {
      Matcher filterMatcher = filterPattern.matcher(entry.getKey());
      if (filterMatcher.find()) {
        filters.put(filterMatcher.group(1), new ArrayList<>(Arrays.asList(entry.getValue())));
      }
    }

    // Check for bounding box
    String[] boundingBoxParam = ctx().request().queryString().get("boundingBox");
    if (boundingBoxParam != null && boundingBoxParam.length > 0) {
      String boundingBox = boundingBoxParam[0];
      if (boundingBox != null) {
        try {
          queryContext.setBoundingBox(boundingBox);
        } catch (NumberFormatException e) {
          Logger.trace("Invalid bounding box: ".concat(boundingBox), e);
        }
      }
    }

    // Sort by dateCreated if no query string given
    if (StringUtils.isEmpty(q) && StringUtils.isEmpty(sort)) {
      sort = "dateCreated:DESC";
    }

    queryContext.setElasticsearchFieldBoosts(new SearchConfig().getBoostsForElasticsearch());

    ResourceList resourceList = mBaseRepository.query(q, from, size, sort, filters, queryContext);

    String baseUrl = mConf.getString("proxy.host");
    String filterString = "";
    for (Map.Entry<String, List<String>> filter : filters.entrySet()) {
      String filterKey = "filter.".concat(filter.getKey());
      for (String filterValue : filter.getValue()) {
        filterString = filterString.concat("&".concat(filterKey).concat("=").concat(filterValue));
      }
    }

    Set<String> alternates = MimeTypes.all().keySet();
    if (!resourceList.containsType("Event")) {
      alternates.remove("ics");
    }
    List<String> links = new ArrayList<>();
    for (String alternate : alternates) {
      String linkUrl = baseUrl.concat(routes.ResourceIndex.list(q, 0, 9999, sort, list, alternate, iso3166)
        .url().concat(filterString));
      links.add(String.format("<%s>; rel=\"alternate\"; type=\"%s\"", linkUrl, MimeTypes.fromExtension(alternate)));
    }

    response().setHeader("Link", String.join(", ", links));

    String format = StringUtils.isEmpty(extension)
      ? MimeTypes.fromRequest(request())
      : MimeTypes.fromExtension(extension);

    if (format == null) {
      return notFound("Not found");
    } else if (format.equals("text/csv")) {
      return ok(new CsvWithNestedIdsExporter().export(resourceList)).as("text/csv");
    } else if (format.equals("text/calendar")) {
      return ok(new CalendarExporter(Locale.ENGLISH).export(resourceList)).as("text/calendar");
    } else if (format.equals("application/json")) {
      Resource result = resourceList.toResource();
      if (!StringUtils.isEmpty(iso3166)) {
        if (!StringUtils.isEmpty(iso3166)) {
          result.put("iso3166", iso3166);
        }
      }
      return ok(result.toString()).as("application/json");
    } else if (format.equals("application/geo+json")) {
      return ok(mGeoJsonExporter.export(resourceList)).as("application/geo+json");
    } else if (format.equals("application/schema+json")) {
      return ok(new JsonSchemaExporter().export(resourceList)).as("application/schema+json");
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
    ProcessingReport processingReport = validate(staged);
    if (!processingReport.isSuccess()) {
      ListProcessingReport listProcessingReport = new ListProcessingReport();
      try {
        listProcessingReport.mergeWith(processingReport);
      } catch (ProcessingException e) {
        Logger.warn("Failed to create list processing report", e);
      }
      return badRequest(listProcessingReport.asJson());
    }

    // Save
    mBaseRepository.addResource(resource, getMetadata());

    // Respond
    if (isUpdate) {
      return ok(getRecord(mBaseRepository.getResource(resource.getId())).toJson());
    } else {
      response().setHeader(LOCATION, routes.ResourceIndex.read(resource.getId(), "HEAD", null)
        .absoluteURL(request()));
      return created(getRecord(mBaseRepository.getResource(resource.getId())).toJson());
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
        ProcessingReport processingMessages = validate(staged);
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

    mBaseRepository.addResources(resources, getMetadata());

    return ok("Added resources");

  }

  public Result read(String id, String version, String extension) throws IOException {
    Resource resource = mBaseRepository.getResource(id, version);
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
        return ok(resource.toJson());
      }
    }

    String baseUrl = mConf.getString("proxy.host");
    Set<String> alternates = MimeTypes.all().keySet();
    if (!resource.getType().equals("Event")) {
      alternates.remove("ics");
    }
    List<String> links = new ArrayList<>();
    for (String alternate : alternates) {
      String linkUrl = baseUrl.concat(routes.ResourceIndex.read(id, version, alternate).url());
      links.add(String.format("<%s>; rel=\"alternate\"; type=\"%s\"", linkUrl, MimeTypes.fromExtension(alternate)));
    }

    response().setHeader("Link", String.join(", ", links));

    String format = StringUtils.isEmpty(extension)
      ? MimeTypes.fromRequest(request())
      : MimeTypes.fromExtension(extension);

    resource = getRecord(resource);

    if (format == null) {
      return notFound("Not found");
    } else if (format.equals("application/json")) {
      return ok(resource.toString()).as("application/json");
    } else if (format.equals("text/csv")) {
      return ok(new CsvWithNestedIdsExporter().export(resource)).as("text/csv");
    } else if (request().accepts("application/geo+json")) {
      return ok(mGeoJsonExporter.export(resource));
    } else if (format.equals("application/schema+json")) {
      return ok(new JsonSchemaExporter().export(resource)).as("application/schema+json");
    } else if (format.equals("text/calendar")) {
      String ical = new CalendarExporter(Locale.ENGLISH).export(resource);
      if (ical != null) {
        return ok(ical).as("text/calendar");
      }
    }

    return notFound("Not found");

  }

  private Record getRecord(Resource aResource) {
    Record record = new Record(aResource);
    List<Commit> history = mBaseRepository.log(aResource.getId());
    record.put(Record.CONTRIBUTOR, history.get(0).getHeader().getAuthor());
    try {
      record.put(Record.AUTHOR, history.get(history.size() - 1).getHeader().getAuthor());
    } catch (NullPointerException e) {
      Logger.trace("Could not read author from commit " + history.get(history.size() - 1), e);
    }
    record.put(Record.DATE_MODIFIED, history.get(0).getHeader().getTimestamp()
      .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    try {
      record.put(Record.DATE_CREATED, history.get(history.size() - 1).getHeader().getTimestamp()
        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    } catch (NullPointerException e) {
      Logger.trace("Could not read timestamp from commit " + history.get(history.size() - 1), e);
    }
    return record;
  }

  public Result delete(String aId) throws IOException {
    Resource resource = mBaseRepository.deleteResource(aId, getMetadata());
    if (null != resource) {
      // If deleting personal profile, also delete corresponding user
      if ("Person".equals(resource.getType())) {
        String username = mAccountService.getUsername(aId);
        if (!mAccountService.removePermissions(aId)) {
          Logger.error("Could not remove permissions for " + aId);
        }
        if (!mAccountService.setProfileId(username, null)) {
          Logger.error("Could not unset profile ID for " + username);
        }
        if (!mAccountService.deleteUser(username)) {
          Logger.error("Could not delete user " + username);
        }
        return ok("deleted user " + aId);
      } else {
        return ok("deleted resource " + aId);
      }
    } else {
      return badRequest("Failed to delete resource " + aId);
    }
  }

  public Result log(String aId) throws IOException {

    Map<String, Object> scope = new HashMap<>();
    scope.put("commits", mBaseRepository.log(aId));
    scope.put("resource", aId);

    if (StringUtils.isEmpty(aId)) {
      return ok(mBaseRepository.log(aId).toString());
    }
    return ok(mObjectMapper.writeValueAsString(scope));

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

  public Result feed() throws IOException {

    ResourceList resourceList = mBaseRepository.query("", 0, 20, "dateCreated:DESC", null, getQueryContext());
    Map<String, Object> scope = new HashMap<>();
    scope.put("resources", resourceList.toResource());

    return ok(mObjectMapper.writeValueAsString(scope));

  }

  public Result label(String aId) {

    return ok(mBaseRepository.label(aId));

  }

}
