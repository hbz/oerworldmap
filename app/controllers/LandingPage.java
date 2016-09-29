package controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import models.Resource;
import models.ResourceList;
import play.Configuration;
import play.Environment;
import play.mvc.Result;
import services.AggregationProvider;
import services.QueryContext;

import javax.inject.Inject;

public class LandingPage extends OERWorldMap {

  @Inject
  public LandingPage(Configuration aConf, Environment aEnv) {
    super(aConf, aEnv);
  }

  public Result get() throws IOException {

    Resource typeAggregation = mBaseRepository.aggregate(AggregationProvider.getTypeAggregation(0),
        new QueryContext(null));
    Map<String, Object> scope = new HashMap<>();
    scope.put("typeAggregation", typeAggregation);

    return ok(render("OER World Map", "LandingPage/index.mustache", scope));

  }

}
