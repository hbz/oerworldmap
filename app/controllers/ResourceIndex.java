package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import helpers.Countries;
import helpers.JSONForm;
import models.Resource;
import play.mvc.Result;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author fo
 */
public class ResourceIndex extends OERWorldMap {

  public static Result list() {
    Map<String,Object> scope = new HashMap<>();
    scope.put("countries", Countries.list(currentLocale));
    return ok(render("Resources", "ResourceIndex/index.mustache", scope));
  }

  public static Result create() throws IOException {
    boolean isJsonRequest = true;
    JsonNode json = request().body().asJson();
    if (null == json) {
      json = JSONForm.parseFormData(request().body().asFormUrlEncoded());
      isJsonRequest = false;
    }
    Resource resource = Resource.fromJson(json);
    ProcessingReport report = resource.validate();
    if (!report.isSuccess()) {
      Map<String,Object> scope = new HashMap<>();
      scope.put("resource", resource);
      scope.put("countries", Countries.list(currentLocale));
      if (isJsonRequest) {
        return badRequest(report.toString());
      } else {
        return badRequest(render("Resources", "ResourceIndex/index.mustache", scope,
            JSONForm.generateErrorReport(report)));
      }
    }
    mBaseRepository.addResource(resource);
    return created("created resource " + resource.toString());
  }

}
