package controllers;

import org.apache.commons.lang3.StringUtils;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
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
      // FIXME: using this is the new way, but drops any headers subsequently set in controllers
      //return delegate.call(new Http.Context(ctx.request().withUsername(username)));
      ctx.request().setUsername(username);
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
