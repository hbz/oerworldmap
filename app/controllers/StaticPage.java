package controllers;

import org.apache.commons.lang3.StringEscapeUtils;
import play.mvc.Result;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * @author fo
 */
public class StaticPage extends OERWorldMap {

  public static Result get(String aPage)  {

    ResourceBundle page;
    try {
      page = ResourceBundle.getBundle(
          aPage.substring(0, 1).toUpperCase() + aPage.substring(1).concat("Page").concat("Bundle"), currentLocale);
    } catch (MissingResourceException e) {
      return notFound("Page not found");
    }

    String title;
    String body;
    try {
      title = StringEscapeUtils.unescapeJava(
          new String(page.getString("title").getBytes("ISO-8859-1"), "UTF-8"));
      body = StringEscapeUtils.unescapeJava(
          new String(page.getString("body").getBytes("ISO-8859-1"), "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      title = page.getString("title");
      body = page.getString("body");
    }

    Map<String,Object> scope = new HashMap<>();
    scope.put("title", title);
    scope.put("body", body);
    return ok(render(page.getString("title"), "StaticPage/index.mustache", scope));
  }

}
