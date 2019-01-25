package controllers;

import play.Configuration;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author fo
 */
public class Robots extends Controller {

  Configuration mConf;

  @Inject
  public Robots(Configuration aConf) {
    mConf = aConf;
  }

  public Result get() {
    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter, true);
    if (mConf.getBoolean("web.crawling.allowed")) {
      writer.println("User-agent: *");
      writer.println("Disallow:");
    } else {
      writer.println("User-agent: *");
      writer.println("Disallow: /");
    }
    return ok(stringWriter.toString());
  }
}
