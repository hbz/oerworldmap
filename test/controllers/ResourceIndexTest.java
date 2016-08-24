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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import helpers.ElasticsearchTestGrid;
import helpers.JsonLdConstants;
import helpers.JsonTest;
import models.Resource;
import play.mvc.Result;
import play.test.FakeApplication;

/**
 * @author fo
 */
public class ResourceIndexTest extends ElasticsearchTestGrid implements JsonTest {

  //FIXME: Authorization is now done by external means, should we test this here and if so, how?

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
        Map<String, String> data = new HashMap<>();
        data.put(JsonLdConstants.TYPE, "Organization");
        data.put(JsonLdConstants.ID, "info:urn:uuid:" + UUID.randomUUID().toString());
        data.put("email", "foo1@bar.com");
        data.put("name[0][@value]", "Foo");
        data.put("name[0][@language]", "en");
        data.put(JsonLdConstants.CONTEXT, "http://schema.org/");
        Result result = route(fakeRequest("POST", routes.ResourceIndex.addResource().url())
            .bodyForm(data));
        assertEquals(201, status(result));
      }
    });
  }

  @Test
  public void createResourceFromJson() {
    running(fakeApplication, new Runnable() {
      @Override
      public void run() {
        Resource event = getResourceFromJsonFileUnsafe("ResourceIndexTest/testEvent.json");
        Result result = route(fakeRequest("POST", routes.ResourceIndex.addResource().url())
            .bodyJson(event.toJson()));
        assertEquals(201, status(result));
      }
    });
  }

  @Test
  public void updateResourceFromJson() {
    running(fakeApplication, new Runnable() {
      @Override
      public void run() {

        Resource event = getResourceFromJsonFileUnsafe("ResourceIndexTest/testEvent.json");
        Result createEventResult = route(fakeRequest("POST", routes.ResourceIndex.addResource().url())
          .bodyJson(event.toJson()));
        assertEquals(201, status(createEventResult));

        Resource organization = getResourceFromJsonFileUnsafe("ResourceIndexTest/testOrganization.json");
        Result createOrganizationResult = route(fakeRequest("POST", routes.ResourceIndex.addResource().url())
          .bodyJson(organization.toJson()));
        assertEquals(201, status(createOrganizationResult));

        organization.put("email", "foo@bar.de");
        Result updateResult = route(fakeRequest("POST", routes.ResourceIndex.updateResource(organization.getId()).url())
          .bodyJson(organization.toJson()));
        assertEquals(200, status(updateResult));
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
        Result updateResult = route(fakeRequest("POST", routes.ResourceIndex.updateResource(organization.getId()).url())
          .header("Authorization", "Basic " + auth).bodyJson(organization.toJson()));
        assertEquals(404, status(updateResult));
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
