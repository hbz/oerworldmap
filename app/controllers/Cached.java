package controllers;

import org.apache.commons.lang3.StringUtils;
import play.Logger;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Created by fo on 25.10.17.
 */
public class Cached extends Action.Simple {

  public static Date lastModified = new Date();

  private static final String CACHE_CONTROL = "Cache-Control";
  private static final String LAST_MODIFIED = "Last-Modified";
  private static final String IF_MODIFIED_SINCE = "If-Modified-Since";

  private static final SimpleDateFormat mDateFormat = new SimpleDateFormat("EEE, d MMM yyyy hh:mm:ss z");
  static {
    mDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  @Override
  public CompletionStage<Result> call(Http.Context ctx) {

    String modifiedSince = ctx.request().getHeader(IF_MODIFIED_SINCE);
    if (!StringUtils.isEmpty(modifiedSince)) {
      try {
        if (lastModified.before(mDateFormat.parse(modifiedSince))) {
          return CompletableFuture.completedFuture(status(304));
        }
      } catch (ParseException e) {
        Logger.info("Invalid " + IF_MODIFIED_SINCE, e);
      }
    }

    ctx.response().setHeader(CACHE_CONTROL, "private");
    ctx.response().setHeader(LAST_MODIFIED, mDateFormat.format(lastModified));
    return delegate.call(ctx);

  }

}
