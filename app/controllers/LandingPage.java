package controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import models.Resource;
import play.mvc.Result;
import services.AggregationProvider;
import services.QueryContext;

public class LandingPage extends OERWorldMap {

  public static Result get() throws IOException {

    Map<String, Object> scope = new HashMap<>();
    Resource typeAggregation = mBaseRepository.aggregate(AggregationProvider.getTypeAggregation(), new QueryContext(ctx()));
    scope.put("typeAggregation", typeAggregation);
    return ok(render("OER World Map", "LandingPage/index.mustache", scope));

  }

}
