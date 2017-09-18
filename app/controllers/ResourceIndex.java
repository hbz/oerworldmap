package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import helpers.JSONForm;
import helpers.JsonLdConstants;
import helpers.SCHEMA;
import helpers.UniversalFunctions;
import models.Record;
import models.Resource;
import models.ResourceList;
import models.TripleCommit;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.ResourceFactory;
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

/**
 * @author fo
 */
public class ResourceIndex extends IndexCommon {

  @Inject
  public ResourceIndex(Configuration aConf, Environment aEnv) {
    super(aConf, aEnv);
  }

  @Override
  protected Result upsertResource(boolean isUpdate) throws IOException {
    String[] forbiddenTypes = new String[]{"Person"};
    final ReverseResourceIndex resourceIndex = routes.ResourceIndex;
    return upsertResource(isUpdate, resourceIndex, forbiddenTypes);
  }

  @Override
  protected Result upsertResources() throws IOException {
    String[] forbiddenTypes = new String[]{"Person"};
    return upsertResources(forbiddenTypes);
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
      filters.put(Record.RESOURCE_KEY + ".countryChampionFor", Arrays.asList(iso3166.toLowerCase()));
      ResourceList champions = mBaseRepository.query("*", 0, 9999, null, filters);
      ResourceList reports = mBaseRepository.query(
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
    ResourceList resourceList =
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
      return ok(resourceList.toResource().toString()).as("application/json");
    }
    else if (format.equals("application/geo+json")) {
      return ok(new GeoJsonExporter().export(resourceList)).as("application/geo+json");
    }

    return notFound("Not found");

  }

  public Result importResources() throws IOException {
    return importResources();
  }


  public Result updateResource(String aId) throws IOException {
    return super.updateResource(aId);
  }

  public Result readDefault(String id, String version) throws IOException {
    return read(id, version, null);
  }

  // TODO: drop this method and add second parameter for delete() to route
  public Result delete(String aId) throws IOException {
    return delete(aId, Record.TYPE);
  }


  public Result delete(final String aId, final String aClassType) throws IOException {
    Resource resource = mBaseRepository.deleteResource(aId, aClassType, getMetadata());
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
    Resource comment = Resource.fromJson(jsonNode);

    comment.put("author", getUser());
    comment.put("dateCreated", ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

    TripleCommit.Diff diff = (TripleCommit.Diff) mBaseRepository.getDiff(comment);
    diff.addStatement(ResourceFactory.createStatement(
      ResourceFactory.createResource(aId), SCHEMA.comment, ResourceFactory.createResource(comment.getId())
    ));

    Map<String, Object> metadata = getMetadata();
    TripleCommit.Header header = new TripleCommit.Header(metadata.get(TripleCommit.Header.AUTHOR_HEADER).toString(),
      ZonedDateTime.parse((CharSequence)metadata.get(TripleCommit.Header.DATE_HEADER)));
    TripleCommit commit = new TripleCommit(header, diff);
    mBaseRepository.commit(commit);

    return seeOther("/resource/" + aId);

  }

  public Result feed() {
    String[] indices = new String[]{mConf.getString("es.index.webpage.name")};
    ResourceList resourceList =
      mBaseRepository.query("", 0, 20, "dateCreated:DESC", null, getQueryContext(), indices);
    Map<String, Object> scope = new HashMap<>();
    scope.put("resources", resourceList.toResource());

    return ok(render("OER World Map", "ResourceIndex/feed.mustache", scope));

  }

}
