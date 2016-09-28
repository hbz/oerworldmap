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
class Authorized extends Action.Simple {

  private static final String AUTHORIZATION = "authorization";

  @Override
  public F.Promise<Result> call(Http.Context ctx) throws Throwable {

    String username = getHttpBasicAuthUser(ctx);

    if (!StringUtils.isEmpty(username)) {
      ctx.request().withUsername(username);
    }

    return delegate.call(ctx);

  }

  private String getHttpBasicAuthUser(Http.Context ctx) {

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
