package controllers;

import play.Environment;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author fo
 */
public class Robots extends Controller {

  private Environment mEnv;

  @Inject
  public Robots(Environment aEnv) {
    mEnv = aEnv;
  }

  public Result get() {
    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter, true);
    if (mEnv.isProd()) {
      writer.println("User-agent: *");
      writer.println("Disallow:");
    } else {
      writer.println("User-agent: *");
      writer.println("Disallow: /");
    }
    return ok(stringWriter.toString());
  }

}
