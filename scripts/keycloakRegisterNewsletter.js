SimpleHttp = Java.type("org.keycloak.broker.provider.util.SimpleHttp");
AuthenticationFlowError = Java.type("org.keycloak.authentication.AuthenticationFlowError");

var registerNewsletterURL = "https://listen.hbz-nrw.de/mailman/subscribe/oerworldmap"

function requiresUser() {
  return true;
}

function authenticate(context) {
  if (user.getAttribute("subscribeNewsletter").contains("true")) {
    // subscribe user to newsletter
    res = SimpleHttp.doPost(registerNewsletterURL, session)
      .param("email", user.email)
      .param("fullname", user.getFirstName() + " " + user.getLastName())
      .asStatus();
    if (res !== 200) {
      LOG.error("Failed to register newsletter for " + user.username);
    } else {
      LOG.info("Registered newsletter for " + user.username);
    }
  }
  return context.success();
}
