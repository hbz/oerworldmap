package controllers;

import org.apache.commons.lang3.StringUtils;
import play.Logger;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Created by fo on 25.10.17.
 */
public class Cached extends Action.Simple {

  private static ZonedDateTime mLastModified;

  private static final String CACHE_CONTROL = "Cache-Control";
  private static final String LAST_MODIFIED = "Last-Modified";
  private static final String IF_MODIFIED_SINCE = "If-Modified-Since";

  private static DateTimeFormatter mDateTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME;

  static {
    updateLastModified();
  }

  @Override
  public CompletionStage<Result> call(Http.Context ctx) {

    String ifModifiedSince = ctx.request().getHeader(IF_MODIFIED_SINCE);
    if (!StringUtils.isEmpty(ifModifiedSince)) {
      try {
        ZonedDateTime modifiedSince = ZonedDateTime.parse(ifModifiedSince, mDateTimeFormatter)
          .truncatedTo(ChronoUnit.SECONDS);
        if (!mLastModified.isAfter(modifiedSince)) {
          return CompletableFuture.completedFuture(status(304));
        }
      } catch (DateTimeParseException e) {
        Logger.info("Invalid " + IF_MODIFIED_SINCE, e);
      }
    }

    ctx.response().setHeader(CACHE_CONTROL, "private");
    ctx.response().setHeader(LAST_MODIFIED, mDateTimeFormatter.format(mLastModified));
    return delegate.call(ctx);

  }

  static void updateLastModified() {
    mLastModified = ZonedDateTime.now(ZoneId.of("GMT")).truncatedTo(ChronoUnit.SECONDS);
  }

}
