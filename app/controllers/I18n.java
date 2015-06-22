package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import play.mvc.Result;

/**
 * @author fo
 */
public class I18n extends OERWorldMap {

  public static Result get() {
    String countryMap = new ObjectMapper().convertValue(i18n, JsonNode.class).toString();
    return ok("i18n = ".concat(countryMap)).as("application/javascript");
  }

}
