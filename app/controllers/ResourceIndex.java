package controllers;

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
    Map<String,Object> scope = new HashMap<>();
    Resource resource = Resource.fromJson(JSONForm.parseFormData(request().body().asFormUrlEncoded()));
    scope.put("resource", resource);
    scope.put("countries", Countries.list(currentLocale));
    ProcessingReport report = resource.validate();
    if (!report.isSuccess()) {
      return badRequest(render("Resources", "ResourceIndex/index.mustache", scope,
          JSONForm.generateErrorReport(report)));
    }
    return created("created resource " + resource.toString());
  }

}
