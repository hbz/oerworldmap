package controllers;

import org.apache.commons.lang3.StringUtils;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Created by fo on 25.10.17.
 */
public class Cached extends Action.Simple {

  private static String mEtag;

  private static final String CACHE_CONTROL = "Cache-Control";
  private static final String ETAG = "Etag";
  private static final String IF_NONE_MATCH = "If-None-Match";

  static {
    updateEtag();
  }

  @Override
  public CompletionStage<Result> call(Http.Context ctx) {
    String ifNoneMatch = ctx.request().getHeader(IF_NONE_MATCH);
    if (!StringUtils.isEmpty(ifNoneMatch) && mEtag.equals(ifNoneMatch)) {
      return CompletableFuture.completedFuture(status(304));
    }

    ctx.response().setHeader(CACHE_CONTROL, "private");
    ctx.response().setHeader(ETAG, mEtag);
    return delegate.call(ctx);
  }

  static void updateEtag() {
    mEtag = UUID.randomUUID().toString();
  }
}
