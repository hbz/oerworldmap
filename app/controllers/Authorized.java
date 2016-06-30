package controllers;

import models.Resource;
import org.apache.commons.lang3.StringUtils;
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

    Resource user = null;

    List<String> roles = new ArrayList<>();
    roles.add("guest");

    if (!StringUtils.isEmpty(username)) {
      ctx.request().setUsername(username);
      String profileId = OERWorldMap.getAccountService().getProfileId(username);
      if (!StringUtils.isEmpty(profileId)) {
        roles.add("authenticated");
        user = OERWorldMap.getRepository().getResource(profileId);
      }
    }

    ctx.args.put("user", user);
    ctx.args.put("username", username);

    // TODO: get roles? or allowed methods for this url?
    ctx.args.put("queryContext", new QueryContext(roles));

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
