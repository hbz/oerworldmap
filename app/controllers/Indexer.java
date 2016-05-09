package controllers;

import java.io.IOException;

import play.mvc.Result;
import play.twirl.api.Html;

public class Indexer extends OERWorldMap {

  public static Result index() throws IOException {

    // TODO: implement indexing
    return ok(new Html("OER World Map : Indexing OK"));

  }

}
