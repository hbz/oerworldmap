package controllers;

import static org.junit.Assert.assertEquals;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;
import static play.test.Helpers.running;
import static play.test.Helpers.start;
import static play.test.Helpers.status;
import static play.test.Helpers.stop;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import helpers.ElasticsearchTestGrid;
import helpers.JsonTest;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import helpers.JsonLdConstants;
import models.Resource;
import play.mvc.Result;
import play.test.FakeApplication;

/**
 * @author fo
 */
public class ResourceIndexTest extends ElasticsearchTestGrid implements JsonTest {

  private static FakeApplication fakeApplication;

  @BeforeClass
  public static void startFakeApplication() {
    fakeApplication = fakeApplication();
    start(fakeApplication);
  }

  @AfterClass
  public static void shutdownFakeApplication() {
    stop(fakeApplication);
  }

  @Test
  public void createResourceFromFormUrlEncoded() {
    running(fakeApplication, new Runnable() {
      @Override
      public void run() {
        String auth = getAuthString();
        Map<String, String> data = new HashMap<>();
        data.put(JsonLdConstants.TYPE, "Organization");
        data.put(JsonLdConstants.ID, "urn:uuid:" + UUID.randomUUID().toString());
        data.put("email", "foo1@bar.com");
        data.put("name[0][@value]", "Foo");
        data.put("name[0][@language]", "en");
        Result result = route(fakeRequest("POST", routes.ResourceIndex.create().url())
            .withHeader("Authorization", "Basic " + auth).withFormUrlEncodedBody(data));
        assertEquals(201, status(result));
      }
    });
  }

  @Test
  public void createResourceFromJson() {
    running(fakeApplication, new Runnable() {
      @Override
      public void run() {
        String auth = getAuthString();
        Resource organization = getResourceFromJsonFileUnsafe("SchemaTest/testOrganization.json");
        Result result = route(fakeRequest("POST", routes.ResourceIndex.create().url())
            .withHeader("Authorization", "Basic " + auth).withJsonBody(organization.toJson()));
        assertEquals(201, status(result));
      }
    });
  }

  @Test
  public void updateResourceFromJson() {
    running(fakeApplication, new Runnable() {
      @Override
      public void run() {
        Resource organization = getResourceFromJsonFileUnsafe("SchemaTest/testOrganization.json");
        String auth = getAuthString();
        Result createResult = route(fakeRequest("POST", routes.ResourceIndex.create().url())
          .withHeader("Authorization", "Basic " + auth).withJsonBody(organization.toJson()));
        assertEquals(201, status(createResult));
        Result updateResult = route(fakeRequest("POST", routes.ResourceIndex.update(organization.getId()).url())
            .withHeader("Authorization", "Basic " + auth).withJsonBody(organization.toJson()));
        assertEquals(200, status(updateResult));
      }
    });
  }

  // FIXME when new authorization is in place
  //@Test
  public void createPersonFromJsonAuthorized() {
    running(fakeApplication, new Runnable() {
      @Override
      public void run() {
        String auth = getAuthString();
        Resource person = new Resource("Person", Global.getConfig().getString("admin.user"));
        person.put("email", Global.getConfig().getString("admin.user"));
        Result createResult = route(fakeRequest("POST", routes.UserIndex.create().url())
          .withHeader("Authorization", "Basic " + auth).withJsonBody(person.toJson()));
        assertEquals(201, status(createResult));
      }
    });
  }

  // FIXME when new authorization is in place
  //@Test
  public void updatePersonFromJsonAuthorized() {
    running(fakeApplication, new Runnable() {
      @Override
      public void run() {
        String auth = getAuthString();
        Resource person = new Resource("Person", Global.getConfig().getString("admin.user"));
        person.put("email", Global.getConfig().getString("admin.user"));
        Result createResult = route(fakeRequest("POST", routes.UserIndex.create().url())
          .withHeader("Authorization", "Basic " + auth).withJsonBody(person.toJson()));
        assertEquals(201, status(createResult));
        Result updateResult = route(fakeRequest("POST", routes.UserIndex.update(person.getId()).url())
          .withHeader("Authorization", "Basic " + auth).withJsonBody(person.toJson()));
        assertEquals(200, status(updateResult));
      }
    });
  }

  // FIXME when new authorization is in place
  //@Test
  public void createPersonFromJsonUnauthorized() {
    running(fakeApplication, new Runnable() {
      @Override
      public void run() {
        Resource person = new Resource("Person");
        person.put("email", "foo@bar.de");
        Result createResult = route(fakeRequest("POST", routes.UserIndex.create().url())
          .withHeader("Authorization", "Basic ").withJsonBody(person.toJson()));
        assertEquals(401, status(createResult));
      }
    });
  }

  // FIXME when new authorization is in place
  // @Test
  public void updatePersonFromJsonUnauthorized() {
    running(fakeApplication, new Runnable() {
      @Override
      public void run() {
        String auth = getAuthString();
        Resource person = new Resource("Person");
        person.put("email", "foo@bar.de");
        Result createResult = route(fakeRequest("POST", routes.UserIndex.create().url())
          .withHeader("Authorization", "Basic " + auth).withJsonBody(person.toJson()));
        assertEquals(201, status(createResult));
        Result updateResult = route(fakeRequest("POST", routes.UserIndex.update(person.getId()).url())
          .withHeader("Authorization", "Basic ").withJsonBody(person.toJson()));
        assertEquals(401, status(updateResult));
      }
    });
  }

  @Test
  public void updateNonexistentResourceFromJson() {
    running(fakeApplication, new Runnable() {
      @Override
      public void run() {
        Resource organization = getResourceFromJsonFileUnsafe("SchemaTest/testOrganization.json");
        String auth = getAuthString();
        Result updateResult = route(fakeRequest("POST", routes.ResourceIndex.update(organization.getId()).url())
            .withHeader("Authorization", "Basic " + auth).withJsonBody(organization.toJson()));
        assertEquals(400, status(updateResult));
      }
    });
  }


  private String getAuthString() {
    String email = Global.getConfig().getString("admin.user");
    String pass = Global.getConfig().getString("admin.pass");
    String authString = email.concat(":").concat(pass);
    return Base64.getEncoder().encodeToString(authString.getBytes());
  }

}
