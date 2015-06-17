package controllers;

import play.mvc.Result;

import java.io.IOException;


public class LandingPage extends OERWorldMap {

  public static Result get() {

    return ok(render("OER World Map", "LandingPage/index.mustache"));

  }

}
