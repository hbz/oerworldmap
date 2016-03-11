package controllers;

import models.Resource;
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

    if (username != null) {
      ctx.request().setUsername(username);
      List<Resource> users = OERWorldMap.getRepository().getResources("about.email", username);
      if (users.size() == 1) {
        user = users.get(0);
      } else {
        user = new Resource("Person", username);
        user.put("email", username);
      }
    } else {
      user = new Resource("Person");
    }

    ctx.args.put("user", user);

    // TODO: get roles? or allowed methods for this url?
    ctx.args.put("queryContext", new QueryContext(user.getId(), new ArrayList<>()));

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
