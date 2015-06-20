package helpers;

import controllers.Global;

public class FilesConfig {

  public static String getSchema() {
    return "public/json/schema.json";
  }

  public static String getOrganizationJsonld() {
    return "public/json/ld/organization.jsonld";
  }

  public static String getRepo() {
    return Global.getConfig().getString("filerepo.dir");
  }

}
