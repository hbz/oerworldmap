package controllers;

import com.typesafe.config.ConfigFactory;
import helpers.ElasticsearchTestGrid;
import helpers.JsonLdConstants;
import helpers.JsonTest;
import models.Resource;
import org.junit.Test;
import play.Configuration;
import play.mvc.Result;

import java.io.File;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;

/**
 * @author fo
 */
public class ResourceIndexTest extends ElasticsearchTestGrid implements JsonTest {

  //FIXME: Authorization is now done by external means, should we test this here and if so, how?

  @Test
  public void createResourceFromJson() {

    Resource event = getResourceFromJsonFileUnsafe("ResourceIndexTest/testEvent.json");
    Result result = route(fakeRequest("POST", routes.ResourceIndex.addResource().url())
      .bodyJson(event.toJson()));
    assertEquals(201, result.status());

  }

  @Test
  public void updateResourceFromJson() {
    Resource event = getResourceFromJsonFileUnsafe("ResourceIndexTest/testEvent.json");
    Result createEventResult = route(fakeRequest("POST", routes.ResourceIndex.addResource().url())
      .bodyJson(event.toJson()));
    assertEquals(201, createEventResult.status());

    Resource organization = getResourceFromJsonFileUnsafe("ResourceIndexTest/testOrganization.json");
    Result createOrganizationResult = route(fakeRequest("POST", routes.ResourceIndex.addResource().url())
      .bodyJson(organization.toJson()));
    assertEquals(201, createOrganizationResult.status());

    organization.put("email", "foo@bar.de");
    Result updateResult = route(fakeRequest("POST", routes.ResourceIndex.updateResource(organization.getId()).url())
      .bodyJson(organization.toJson()));
    assertEquals(200, updateResult.status());

  }

  @Test
  public void updateNonexistentResourceFromJson() {

    Resource organization = getResourceFromJsonFileUnsafe("SchemaTest/testOrganization.json");
    String auth = getAuthString();
    Result updateResult = route(fakeRequest("POST", routes.ResourceIndex.updateResource(organization.getId()).url())
      .header("Authorization", "Basic " + auth).bodyJson(organization.toJson()));
    assertEquals(404, updateResult.status());

  }

  private String getAuthString() {
    return Base64.getEncoder().encodeToString("user:pass".getBytes());
  }

}
