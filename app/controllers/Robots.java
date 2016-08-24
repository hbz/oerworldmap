package controllers;

import play.mvc.Controller;
import play.mvc.Result;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author fo
 */
public class Robots extends Controller {

  public Result get() {
    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter, true);
    if (play.Play.isProd()) {
      writer.println("User-agent: *");
      writer.println("Disallow:");
    } else {
      writer.println("User-agent: *");
      writer.println("Disallow: /");
    }
    return ok(stringWriter.toString());
  }

}
