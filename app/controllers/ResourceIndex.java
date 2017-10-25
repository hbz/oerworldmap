package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import helpers.JSONForm;
import helpers.JsonLdConstants;
import helpers.SCHEMA;
import helpers.UniversalFunctions;
import models.*;
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

import javax.inject.Inject;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static helpers.UniversalFunctions.getTripleCommitHeaderFromMetadata;

/**
 * @author fo
 */
public class ResourceIndex extends IndexCommon {

  private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Inject
  public ResourceIndex(Configuration aConf, Environment aEnv) {
    super(aConf, aEnv);
  }

  public Result listDefault(String q, int from, int size, String sort, boolean list, String iso3166) throws IOException {
    return list(q, from, size, sort, list, null, iso3166);
  }

  public Result list(String q, int from, int size, String sort, boolean list, String extension, String iso3166)
      throws IOException {

    Map<String, Object> scope = new HashMap<>();
    Map<String, List<String>> filters = new HashMap<>();
    QueryContext queryContext = getQueryContext();

    // Handle ISO 3166 param
    if (!StringUtils.isEmpty(iso3166)) {

      if (! UniversalFunctions.resourceBundleToMap(ResourceBundle
        .getBundle("iso3166-1-alpha-2", getLocale()))
        .containsKey(iso3166.toUpperCase())) {
        return notFound("Not found");
      }

      Resource countryAggregation = mBaseRepository.aggregate(AggregationProvider.getForCountryAggregation(iso3166.toUpperCase(), 0));
      filters.put(Record.CONTENT_KEY + ".countryChampionFor", Arrays.asList(iso3166.toLowerCase()));
      ModelCommonList champions = mBaseRepository.query("*", 0, 9999, null, filters);
      ModelCommonList reports = mBaseRepository.query(
        "about.keywords:\"countryreport:".concat(iso3166.toUpperCase()).concat("\""), 0, 10, null, null);
      filters.clear();

      Map<String, Object> iso3166Scope = new HashMap<>();
      String countryName = ResourceBundle.getBundle("iso3166-1-alpha-2", getLocale())
        .getString(iso3166.toUpperCase());
      iso3166Scope.put("alpha-2", iso3166.toUpperCase());
      iso3166Scope.put("name", countryName);
      iso3166Scope.put("champions", champions.getItems());
      iso3166Scope.put("reports", reports.getItems());
      iso3166Scope.put("countryAggregation", countryAggregation);
      scope.put("iso3166", iso3166Scope);

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

    queryContext.setFetchSource(new String[]{
      "@id", "@type", "dateCreated", "author", "dateModified", "contributor",
      "about.@id", "about.@type", "about.name", "about.alternateName", "about.location", "about.image",
      "about.provider.@id", "about.provider.@type", "about.provider.name", "about.provider.location",
      "about.participant.@id", "about.participant.@type", "about.participant.name", "about.participant.location",
      "about.agent.@id", "about.agent.@type", "about.agent.name", "about.agent.location",
      "about.mentions.@id", "about.mentions.@type", "about.mentions.name", "about.mentions.location",
      "about.mainEntity.@id", "about.mainEntity.@type", "about.mainEntity.name", "about.mainEntity.location",
      "about.startDate", "about.endDate", "about.organizer", "about.description", "about.displayName", "about.email"
    });

    queryContext.setElasticsearchFieldBoosts(new SearchConfig().getBoostsForElasticsearch());

    String[] indices = new String[]{mConf.getString("es.index.webpage.name")};
    ModelCommonList resourceList =
      mBaseRepository.query(q, from, size, sort, filters, queryContext, indices);

    Map<String, String> alternates = new HashMap<>();
    String baseUrl = mConf.getString("proxy.host");
    String filterString = "";
    for (Map.Entry<String, List<String>> filter : filters.entrySet()) {
      String filterKey = "filter.".concat(filter.getKey());
      for (String filterValue : filter.getValue()) {
        filterString = filterString.concat("&".concat(filterKey).concat("=").concat(filterValue));
      }
    }

    alternates.put("JSON", baseUrl.concat(routes.ResourceIndex.list(q, 0, 9999, sort, list, "json", iso3166).url().concat(filterString)));
    alternates.put("CSV", baseUrl.concat(routes.ResourceIndex.list(q, 0, 9999, sort, list, "csv", iso3166).url().concat(filterString)));
    alternates.put("GeoJSON", baseUrl.concat(routes.ResourceIndex.list(q, 0, 9999, sort, list, "geojson", iso3166).url().concat(filterString)));
    if (resourceList.containsType("Event")) {
      alternates.put("iCal", baseUrl.concat(routes.ResourceIndex.list(q, 0, 9999, sort, list, "ics", iso3166).url().concat(filterString)));
    }

    scope.put("list", list);
    scope.put("resources", resourceList.toModelCommon());
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
        case "geojson":
          format = "application/geo+json";
          break;
      }
    } else if (request().accepts("text/html")) {
      format = "text/html";
    } else if (request().accepts("text/csv")) {
      format = "text/csv";
    } else if (request().accepts("text/calendar")) {
      format = "text/calendar";
    } else if (request().accepts("application/geo+json")) {
      format = "application/geo+json";
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
      return ok(resourceList.toModelCommon().toString()).as("application/json");
    }
    else if (format.equals("application/geo+json")) {
      return ok(new GeoJsonExporter().export(resourceList)).as("application/geo+json");
    }

    return notFound("Not found");

  }

  public Result importResources() throws IOException {
    return super.importResources();
  }

  public Result updateItem(String aId) throws IOException {
    return super.updateItem(aId);
  }

  protected Result upsertItem(boolean isUpdate) throws IOException {

    // Extract resource
    Resource resource = new Resource(getJsonFromRequest());
    resource.put(JsonLdConstants.CONTEXT, mConf.getString("jsonld.context"));

    // Person create /update only through UserIndex, which is restricted to admin
    if (!isUpdate && "Person".equals(resource.getType())) {
      return forbidden("Upsert Person forbidden.");
    }

    // Validate
    ModelCommon staged = mBaseRepository.stage(resource);
    ProcessingReport processingReport = staged.validate(mTypes.getSchema(staged.getClass()));
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
        if (Arrays.asList("LikeAction", "LighthouseAction").contains(resource.getType())) {
          response().setHeader(LOCATION, routes.ResourceIndex.readDefault(resource.getAsItem("object").getId(),
            "HEAD").absoluteURL(request()));
        }
        return read(resource.getId(), "HEAD", "html");
      } else {
        return ok("Updated " + resource.getId());
      }
    } else {
      if (Arrays.asList("LikeAction", "LighthouseAction").contains(resource.getType())) {
        response().setHeader(LOCATION, routes.ResourceIndex.readDefault(resource.getAsItem("object").getId(),
          "HEAD").absoluteURL(request()));
      } else {
        response().setHeader(LOCATION, routes.ResourceIndex.readDefault(resource.getId(), "HEAD")
          .absoluteURL(request()));
      }
      if (request().accepts("text/html")) {
        return created(render("Created", "created.mustache", resource));
      } else {
        return created("Created " + resource.getId());
      }
    }
  }

  protected Result upsertItems() throws IOException {

    // Extract resources
    List<ModelCommon> resources = new ArrayList<>();
    for (JsonNode jsonNode : getJsonFromRequest()) {
      Resource resource = new Resource(jsonNode);
      resource.put(JsonLdConstants.CONTEXT, mConf.getString("jsonld.context"));
      resources.add(resource);
    }

    // Validate
    ListProcessingReport listProcessingReport = new ListProcessingReport();
    for (ModelCommon resource : resources) {
      // Person create /update only through UserIndex, which is restricted to admin
      if ("Person".equals(resource.getType())) {
        return forbidden("Upsert Person forbidden.");
      }
      // Stage and validate each resource
      try {
        ModelCommon staged = mBaseRepository.stage(resource);
        ProcessingReport processingMessages = staged.validate(mTypes.getSchema(staged.getClass()));
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

  public Result readDefault(String id, String version) throws IOException {
    return read(id, version, null);
  }


  // TODO: drop this method and add second parameter for delete() to route
  public Result delete(String aId) throws IOException {
    return delete(aId, mBaseRepository.getItem(aId).getClass());
  }

  public Result read(String id, String version, String extension) throws IOException {
    Resource currentUser = getUser();
    ModelCommon resource = mBaseRepository.getItem(id, version);
    if (null == resource) {
      return notFound("Not found");
    }
    String type = resource.get(JsonLdConstants.TYPE).toString();

    if (type.equals("Concept")) {
      ModelCommonList relatedList = mBaseRepository.query("about.about.@id:\"".concat(id)
          .concat("\" OR about.audience.@id:\"").concat(id).concat("\""), 0, 999, null, null);
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
        Resource nestedConceptAggregation = mBaseRepository.aggregate(conceptAggregation);
        resource.put("aggregation", nestedConceptAggregation);
        return ok(render("", "ResourceIndex/ConceptScheme/read.mustache", resource));
      }
    }

    List<ModelCommon> comments = new ArrayList<>();
    for (String commentId : resource.getIdList("comment")) {
      comments.add(mBaseRepository.getItem(commentId));
    }

    String likesQuery = String.format("about.@type: LikeAction AND about.object.@id:\"%s\"", resource.getId());
    int likesCount = mBaseRepository.query(likesQuery, 0, 9999, null, null)
      .getItems().size();

    boolean userLikesResource = false;
    if (currentUser != null) {
      String userLikesQuery = String.format(
        "about.@type: LikeAction AND about.agent.@id:\"%s\" AND about.object.@id:\"%s\"",
        currentUser.getId(), resource.getId()
      );
      userLikesResource = mBaseRepository.query(userLikesQuery, 0, 1, null, null)
        .getItems().size() > 0;
    }

    String lightHousesQuery = String.format("about.@type: LighthouseAction AND about.object.@id:\"%s\"", resource.getId());
    int lighthousesCount = mBaseRepository.query(lightHousesQuery, 0, 9999, null, null)
      .getItems().size();

    ModelCommon userLighthouseResource = null;
    boolean userLighthouseIsset = false;
    if (currentUser != null) {
      String userLighthouseQuery = String.format(
        "about.@type: LighthouseAction AND about.agent.@id:\"%s\" AND about.object.@id:\"%s\"",
        currentUser.getId(), resource.getId()
      );
      List<ModelCommon> userLighthouseResources = mBaseRepository.query(userLighthouseQuery, 0, 1, null, null)
        .getItems();
      if (userLighthouseResources.size() > 0) {
        userLighthouseResource = userLighthouseResources.get(0).getAsItem(Record.CONTENT_KEY);
        userLighthouseIsset = true;
      }
    }

    if (userLighthouseResource == null) {
      userLighthouseResource = new Resource("LighthouseAction");
      userLighthouseResource.remove("@id");
      userLighthouseResource.put("object", resource);
      userLighthouseResource.put("agent", Collections.singletonList(getUser()));
    }

    String title;
    try {
      title = ((Resource) ((ArrayList<?>) resource.get("name")).get(0)).get("@value").toString();
    } catch (NullPointerException e) {
      title = id;
    }

    boolean mayEdit = (currentUser != null) && (!resource.getType().equals("LikeAction")) &&
      ((resource.getType().equals("Person") && currentUser.getId().equals(id)) || (!resource.getType().equals("Person"))
        || mAccountService.getGroups(getHttpBasicAuthUser()).contains("admin"));
    boolean mayLog = (currentUser != null) && (mAccountService.getGroups(getHttpBasicAuthUser()).contains("admin")
        || mAccountService.getGroups(getHttpBasicAuthUser()).contains("editor"));
    boolean mayAdminister = (currentUser != null) && mAccountService.getGroups(getHttpBasicAuthUser()).contains("admin");
    boolean mayComment = (currentUser != null) && (!resource.getType().equals("Person"));
    boolean mayDelete = (currentUser != null) && (resource.getType().equals("Person") && currentUser.getId().equals(id)
        || mAccountService.getGroups(getHttpBasicAuthUser()).contains("admin"));
    boolean mayLike = (currentUser != null) && Arrays.asList("Organization", "Action", "Service").contains(resource.getType());

    Map<String, Object> permissions = new HashMap<>();
    permissions.put("edit", mayEdit);
    permissions.put("log", mayLog);
    permissions.put("administer", mayAdminister);
    permissions.put("comment", mayComment);
    permissions.put("delete", mayDelete);
    permissions.put("like", mayLike);

    Map<String, String> alternates = new HashMap<>();
    String baseUrl = mConf.getString("proxy.host");
    alternates.put("JSON", baseUrl.concat(routes.ResourceIndex.read(id, version, "json").url()));
    alternates.put("CSV", baseUrl.concat(routes.ResourceIndex.read(id, version, "csv").url()));
    if (resource.getType().equals("Event")) {
      alternates.put("iCal", baseUrl.concat(routes.ResourceIndex.read(id, version, "ics").url()));
    }

    List<Commit> history = mBaseRepository.log(id);
    Record record = new Record(resource, mTypes.getIndexType(resource.getClass()));
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

    Map<String, Object> scope = new HashMap<>();
    scope.put("resource", record);
    scope.put("comments", comments);
    scope.put("likes", likesCount);
    scope.put("userLikesResource", userLikesResource);
    scope.put("lighthouses", lighthousesCount);
    scope.put("userLighthouseResource", userLighthouseResource);
    scope.put("userLighthouseIsset", userLighthouseIsset);
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


  public Result delete(final String aId, final Class aClazz) throws IOException {
    ModelCommon resource = mBaseRepository.deleteItem(aId, aClazz, getMetadata());
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
    Resource comment = new Resource(jsonNode);

    comment.put("author", getUser());
    comment.put("dateCreated", ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

    TripleCommit.Diff diff = (TripleCommit.Diff) mBaseRepository.getDiff(comment);
    diff.addStatement(ResourceFactory.createStatement(
      ResourceFactory.createResource(aId), SCHEMA.comment, ResourceFactory.createResource(comment.getId())
    ));

    TripleCommit.Header header = getTripleCommitHeaderFromMetadata(getMetadata());
    TripleCommit commit = new TripleCommit(header, diff);
    mBaseRepository.commit(commit);

    return seeOther("/resource/" + aId);
  }


  public Result likeResource(String aId) throws IOException {

    ModelCommon object = mBaseRepository.getItem(aId);
    Resource agent = getUser();

    if (object == null || agent == null) {
      return badRequest("Object or agent missing");
    }

    String likesQuery = String.format("about.@type: LikeAction AND about.agent.@id:\"%s\" AND about.object.@id:\"%s\"",
      agent.getId(), object.getId());

    List<ModelCommon> existingLikes = mBaseRepository.query(likesQuery, 0, 9999, null, null)
      .getItems();

    if (existingLikes.size() > 0) {
      for (ModelCommon like : existingLikes) {
        mBaseRepository.deleteItem(like.getAsItem(Record.CONTENT_KEY).getId(),
          Action.class, getMetadata());
      }
    } else {
      Resource likeAction = new Resource("LikeAction");
      likeAction.put(JsonLdConstants.CONTEXT, mConf.getString("jsonld.context"));
      likeAction.put("agent", agent);
      likeAction.put("object", object);
      likeAction.put("startTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
      mBaseRepository.addItem(likeAction, getMetadata());
    }
    return seeOther("/resource/" + aId);
  }


  public Result feed() {
    String[] indices = new String[]{mConf.getString("es.index.webpage.name")};
    ModelCommonList resourceList =
      mBaseRepository.query("", 0, 20, "dateCreated:DESC", null, getQueryContext(), indices);
    Map<String, Object> scope = new HashMap<>();
    scope.put("resources", resourceList.toModelCommon());

    return ok(render("OER World Map", "ResourceIndex/feed.mustache", scope));
  }

}
