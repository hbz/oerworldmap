package controllers;

import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

/**
 * @author fo
 */
class Authorized extends Action.Simple {

  private static final String OIDC_CLAIM_SUB = "oidc_claim_sub";
  private static final String OIDC_CLAIM_LEGACYID = "oidc_claim_legacyid";

  @Override
  public CompletionStage<Result> call(Http.Context ctx) {
    if (ctx.request().hasHeader(OIDC_CLAIM_LEGACYID)) {
      ctx.request().setUsername("urn:uuid:" + ctx.request().getHeader(OIDC_CLAIM_LEGACYID));
    } else if (ctx.request().hasHeader(OIDC_CLAIM_SUB)) {
      ctx.request().setUsername("urn:uuid:" + ctx.request().getHeader(OIDC_CLAIM_SUB));
    }
    return delegate.call(ctx);
  }

}
