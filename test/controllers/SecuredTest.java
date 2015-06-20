package controllers;

import static org.junit.Assert.*;
import static play.test.Helpers.*;

import models.Resource;
import org.junit.Test;
import play.mvc.Result;
import services.Account;

import java.util.Base64;

/**
 * @author fo
 */
public class SecuredTest {

  /*@Test
  public void notAuthenticated() {
    running(fakeApplication(), new Runnable() {
      @Override
      public void run() {
        Result result = route(fakeRequest("GET", routes.UserIndex.authControl().url()));
        assertEquals(401, status(result));
      }
    });
  }*/

  /*@Test
  public void authenticated() {
    final Resource user = new Resource("Person");
    final String email = "foo@bar.de";
    user.put("email", email);
    running(fakeApplication(), new Runnable() {
      @Override
      public void run() {
        String token = Account.createTokenFor(user);
        String authString = email + ":" + token;
        String auth = Base64.getEncoder().encodeToString(authString.getBytes());
        Result result = route(fakeRequest("GET", routes.UserIndex.authControl().url())
            .withHeader("Authorization", "Basic " + auth));
        assertEquals(200, status(result));
        Account.removeTokenFor(user);
      }
    });
  }*/

}
