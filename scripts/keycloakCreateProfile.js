SimpleHttp = Java.type("org.keycloak.broker.provider.util.SimpleHttp");
AuthenticationFlowError = Java.type("org.keycloak.authentication.AuthenticationFlowError");

var createUserURL = "http://localhost:3333/post"

function authenticate(context) {

    try {
        var username = user ? user.username : "anonymous";
        LOG.info(script.name + " --> trace auth for: " + username);

        // country is selected
        /*if (!user.getAttribute("country")) {
            throw "No country provided"
        }*/

        // privacyAccepted and termsAccepted
        if (!user.getAttribute("privacyAccepted").contains("true") || !user.getAttribute("termsAccepted").contains("true")) {
            throw "No privacyAccepted provided"
        }

        // Set profile_id attribute
        user.setSingleAttribute("profile_id", "urn:uuid:" + user.getId())

        // Post request to create user profile
        var res = SimpleHttp.doPost(createUserURL, session)
            .param("id", user.getAttribute("profile_id")[0])
            .param("name", user.getFirstName() + " " + user.getLastName())
            .param("country", user.getAttribute("country")[0])
            .param("email", (user.getAttribute("publishEmail").contains("true") && user.email)  || null)
            .param("username", user.username)
            .asString()

        LOG.info(res)

        // subscribeNewsletter
        /*if (user.getAttribute("subscribeNewsletter").contains("true")) {
           // Post request to add user to newssletter
           SimpleHttp.doPost("http://localhost:3333/newsletter", session)
            .param("foo","bar")
            .asString()
        }*/
        context.success();
    } catch(err) {
        LOG.info("Error creating profile", err)
        context.failure(AuthenticationFlowError.INVALID_USER);
    }
}
