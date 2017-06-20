package controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import helpers.Countries;
import models.Record;
import models.Resource;
import models.ResourceList;
import play.Configuration;
import play.Environment;
import play.mvc.Result;
import services.AggregationProvider;
import services.QueryContext;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fo
 */
public class CountryIndex extends OERWorldMap {

  @Inject
  public CountryIndex(Configuration aConf, Environment aEnv) {
    super(aConf, aEnv);
  }

  public Result iso3166() throws IOException {
    return ok("window.iso3166 = ".concat(new ObjectMapper().writeValueAsString(Countries.list(getLocale()))))
      .as("application/javascript");
  }

}
