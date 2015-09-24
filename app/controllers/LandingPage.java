package controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import play.mvc.Result;

public class LandingPage extends OERWorldMap {

  public static Result get() throws IOException {

    Map<String, Object> scope = new HashMap<>();
    return ok(render("OER World Map", "LandingPage/index.mustache", scope));

  }

}
