package controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import models.Resource;
import models.ResourceList;
import play.mvc.Result;
import services.AggregationProvider;
import services.QueryContext;

public class LandingPage extends OERWorldMap {

  public static Result get() throws IOException {

    Resource typeAggregation = mBaseRepository.aggregate(AggregationProvider.getTypeAggregation(0));
    Map<String, Object> scope = new HashMap<>();
    scope.put("typeAggregation", typeAggregation);

    return ok(render("OER World Map", "LandingPage/index.mustache", scope));

  }

}
