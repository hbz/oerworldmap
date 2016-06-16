package controllers;

import java.io.IOException;
import play.mvc.Result;

public class LandingPage extends OERWorldMap {

  public static Result get() throws IOException {

    return ok(render("OER World Map", "LandingPage/index.mustache"));

  }

}
