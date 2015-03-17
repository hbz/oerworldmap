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
      mResponseData.put("errors", JSONForm.generateErrorReport(report));
      mResponseData.put("data", resource);
      System.out.println(resource.toString());
      System.out.println(Integer.parseInt("5"));
      return badRequest(render("Resources", "ResourceIndex/index.mustache"));
    }
    return created("created resource " + resource.toString());
  }

}
