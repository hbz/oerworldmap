package controllers;

import models.Resource;
import play.Logger;
import play.libs.F;
import play.mvc.*;
import services.QueryContext;

import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * @author fo
 */
public class Authorized extends Action.Simple {

  public static final String AUTHORIZATION = "authorization";

  @Override
  public F.Promise<Result> call(Http.Context ctx) throws Throwable {

    String username = getHttpBasicAuthUser(ctx);

    Resource user;

    List<String> roles = new ArrayList<>();
    roles.add("guest");

    if (username != null) {
      roles.add("authenticated");
      ctx.request().setUsername(username);
      List<Resource> users = OERWorldMap.getRepository().getResources("about.email", username);
      if (users.size() == 0) {
        user = users.get(0);
      } else if (users.size() > 0) {
        user = users.get(0);
        Logger.warn(String.format("Multiple profiles for %s detected", username));
      } else {
        user = new Resource("Person");
        user.put("email", username);
      }
    } else {
      user = new Resource("Person");
    }

    ctx.args.put("user", user);
    ctx.args.put("username", username);

    // TODO: get roles? or allowed methods for this url?
    ctx.args.put("queryContext", new QueryContext(user.getId(), roles));

    return delegate.call(ctx);

  }

  private static String getHttpBasicAuthUser(Http.Context ctx) {

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

    return credentials[0];

  }

}
