AuthenticationFlowError = Java.type("org.keycloak.authentication.AuthenticationFlowError");

function requiresUser() {
  return true;
}

function authenticate(context) {
  // private policy accepted
  if (!user.getAttribute("privacyAccepted").contains("true")) {
    LOG.error("Privacy policy not accepted for " + user.username);
    return context.failure(AuthenticationFlowError.INVALID_USER);
  }

  // terms of service accepted
  if (!user.getAttribute("termsAccepted").contains("true")) {
    LOG.error("Terms of service not accepted for " + user.username);
    return context.failure(AuthenticationFlowError.INVALID_USER);
  }

  // store user attributes
  var accepted = (new Date()).toISOString() + " from "
    + httpRequest.getHttpHeaders().getHeaderString("X-Forwarded-For");
  user.setSingleAttribute("privacyAccepted", accepted);
  user.setSingleAttribute("termsAccepted", accepted);

  return context.success();
}
