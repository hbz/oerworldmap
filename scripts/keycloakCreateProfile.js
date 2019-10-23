SimpleHttp = Java.type("org.keycloak.broker.provider.util.SimpleHttp");
AuthenticationFlowError = Java.type("org.keycloak.authentication.AuthenticationFlowError");

var createUserURL = "http://localhost:9000/user/profile"

function requiresUser() {
  return true;
}

function authenticate(context) {
  if (user.getAttribute("profile_id").length) {
    return context.success();
  }

  // create user profile
  var profileId = "urn:uuid:" + user.getId();
  var res = SimpleHttp.doPost(createUserURL, session)
    .param("id", profileId)
    .param("name", user.getFirstName() + " " + user.getLastName())
    .param("country", user.getAttribute("country")[0])
    .param("email", user.getAttribute("publishEmail").contains("true") ? user.email : null)
    .param("username", user.username)
    .asStatus();

  if (res === 201) {
    LOG.info("Created profile for " + user.username);
    // Set user attribute
    user.setSingleAttribute("profile_id", profileId);
    return context.success();
  } else {
    LOG.error("Failed to create profile for " + user.username);
    return context.failure(AuthenticationFlowError.INVALID_USER);
  }
}
