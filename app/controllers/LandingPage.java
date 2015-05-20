package controllers;

import play.mvc.Result;

import java.io.IOException;


public class LandingPage extends OERWorldMap {

  public static Result get() throws IOException {

    return ok(render("Home", "LandingPage/index.mustache"));

  }

}
