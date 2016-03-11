package controllers;

import helpers.ElasticsearchTestGrid;
import models.Resource;
import org.junit.Test;
import play.mvc.Result;
import services.Account;

import java.util.Base64;

import static org.junit.Assert.assertEquals;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;
import static play.test.Helpers.running;
import static play.test.Helpers.status;

/**
 * @author fo
 */
public class AuthorizedTest extends ElasticsearchTestGrid {

  @Test
  public void authenticated() {

    final Resource user = new Resource("Person");
    final String email = "foo@bar.de";
    user.put("email", email);

    running(fakeApplication(), new Runnable() {
      @Override public void run() {
        String token = Account.createTokenFor(user);
        String authString = email + ":" + token;
        String auth = Base64.getEncoder()
          .encodeToString(authString.getBytes());
        Result result = route(fakeRequest("GET", routes.LandingPage.get().url())
          .withHeader("Authorization", "Basic " + auth));
        assertEquals(200, status(result));
        Account.removeTokenFor(user);
      }
    });

  }

}
