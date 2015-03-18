package controllers;

import com.github.fge.jsonschema.core.report.ProcessingReport;
import helpers.JSONForm;
import models.Resource;
import play.mvc.Result;

import java.io.IOException;

/**
 * @author fo
 */
public class ResourceIndex extends OERWorldMap {

  public static Result list() {
    return ok(render("Resources", "ResourceIndex/index.mustache"));
  }

  public static Result create() throws IOException {
    Resource resource = Resource.fromJson(JSONForm.parseFormData(request().body().asFormUrlEncoded()));
    ProcessingReport report = resource.validate();
    if (!report.isSuccess()) {
      return badRequest(render("Resources", "ResourceIndex/index.mustache", resource,
          JSONForm.generateErrorReport(report)));
    }
    return created("created resource " + resource.toString());
  }

}
