package controllers;

import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import play.mvc.Security;
import services.Account;

import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * @author fo
 */
public class Secured extends Security.Authenticator {

  public static final String REALM = "Basic realm=\"OER World Map\"";
  public static final String AUTHORIZATION = "authorization";
  public static final String WWW_AUTHENTICATE = "WWW-Authenticate";

  @Override
  public String getUsername(Http.Context ctx) {
      return getHttpBasicAuthUser(ctx);
  }

  public static String getHttpBasicAuthUser(Http.Context ctx) {

    String authHeader = ctx.request().getHeader(AUTHORIZATION);

    if (null == authHeader) {
      return null;
    }

    String auth = authHeader.substring(6);
    byte[] decoded = Base64.getDecoder().decode(auth);

    String[] credentials;
    try {
      credentials = new String(decoded, "UTF-8").split(":");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      return null;
    }

    if (credentials.length != 2) {
      return null;
    }

    String username = credentials[0];
    String password = credentials[1];

    if (!Account.authenticate(username, password)) {
      return null;
    }

    return username;

  }

  @Override
  public Result onUnauthorized(Http.Context ctx) {

    Map<String, Object> scope = new HashMap<>();
    scope.put("continue", "<a href=\"\">".concat(
      OERWorldMap.messages.getString("feedback_link_continue")).concat("</a>"));

    List<Map<String, Object>> messages = new ArrayList<>();
    HashMap<String, Object> message = new HashMap<>();
    message.put("level", "warning");
    message.put("message", UserIndex.messages.getString("user_status_login_failed"));
    messages.add(message);

    ctx.response().setHeader(WWW_AUTHENTICATE, REALM);
    return unauthorized(OERWorldMap.render("Unauthorized", "feedback.mustache", scope, messages));

  }

}
