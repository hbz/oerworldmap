package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import helpers.JsonLdConstants;
import models.ModelCommon;
import models.Resource;
import org.apache.commons.lang3.StringUtils;
import play.Configuration;
import play.Environment;
import play.Logger;
import play.mvc.Result;
import services.export.CalendarExporter;
import services.export.CsvWithNestedIdsExporter;

import java.io.IOException;
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
    ModelCommon staged = mBaseRepository.stage(resource);
    ProcessingReport processingReport = staged.validate(mTypes.getSchema(staged.getClass()));
    Result badListProcessingReport = addListProcessingReport(resource, processingReport);
    if (badListProcessingReport != null) return badListProcessingReport;

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

  protected Result addListProcessingReport(ModelCommon resource, ProcessingReport processingReport) {
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
    return null;
  }

  protected abstract Result upsertItems() throws IOException;

  protected Result upsertItems(final String... aForbiddenTypes) throws IOException {

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
      for (String forbiddenType : aForbiddenTypes) {
        if (forbiddenType.equals(resource.getType())) {
          return forbidden(String.format("Upsert %s forbidden.", forbiddenType));
        }
      }

      // Stage and validate each resource
      if (!stageAndValidate(listProcessingReport, resource)) {
        return badRequest();
      }
    }

    if (!listProcessingReport.isSuccess()) {
      return badRequest(listProcessingReport.asJson());
    }
    mBaseRepository.addItems(resources, getMetadata());
    return ok("Added resources");
  }

  protected boolean stageAndValidate(ListProcessingReport listProcessingReport, ModelCommon resource) throws IOException {
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
      return false;
    }
    return true;
  }

  public abstract Result read(String id, String version, String extension) throws IOException;

  protected String getFormatString(String extension, String format) {
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
    return format;
  }

  protected Result renderResult(ModelCommon resource, String title, Map<String, Object> scope, String format) {
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
    return null;
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
    ModelCommon originalResource = mBaseRepository.getItem(aId);
    if (originalResource == null) {
      return notFound("Not found: ".concat(aId));
    }
    return upsertItem(true);
  }


  public Result label(String aId) {
    return ok(mBaseRepository.label(aId));
  }
}
