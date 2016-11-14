package controllers;

import org.apache.commons.lang3.StringUtils;
import play.mvc.*;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.CompletionStage;

/**
 * @author fo
 */
class Authorized extends Action.Simple {

  private static final String AUTHORIZATION = "authorization";

  @Override
  public CompletionStage<Result> call(Http.Context ctx) {

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
