package controllers;

import static org.junit.Assert.assertEquals;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;

import helpers.ElasticsearchTestGrid;
import helpers.JsonTest;
import java.util.Base64;
import models.Resource;
import org.junit.Test;
import play.mvc.Result;

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

    Resource organization = getResourceFromJsonFileUnsafe(
      "ResourceIndexTest/testOrganization.json");
    Result createOrganizationResult = route(
      fakeRequest("POST", routes.ResourceIndex.addResource().url())
        .bodyJson(organization.toJson()));
    assertEquals(201, createOrganizationResult.status());

    organization.put("email", "foo@bar.de");
    Result updateResult = route(
      fakeRequest("POST", routes.ResourceIndex.updateResource(organization.getId()).url())
        .bodyJson(organization.toJson()));
    assertEquals(200, updateResult.status());
  }

  @Test
  public void updateNonexistentResourceFromJson() {

    Resource organization = getResourceFromJsonFileUnsafe("SchemaTest/testOrganization.json");
    String auth = getAuthString();
    Result updateResult = route(
      fakeRequest("POST", routes.ResourceIndex.updateResource(organization.getId()).url())
        .header("Authorization", "Basic " + auth).bodyJson(organization.toJson()));
    assertEquals(404, updateResult.status());
  }

  @Test
  public void likeResource() {
    Result result;

    Resource likeObject = getResourceFromJsonFileUnsafe("ResourceIndexTest/testLikeObject.json");
    result = route(fakeRequest("POST", routes.ResourceIndex.addResource().url())
      .bodyJson(likeObject.toJson()));
    assertEquals(201, result.status());

    // FIXME: this currently fails because Person items cannot be added using the ResourceIndex.
    // But where is the FakeApplication that the fakeRequests use configured?
    Resource likeAgent = getResourceFromJsonFileUnsafe("ResourceIndexTest/testLikeAgent.json");
    result = route(fakeRequest("POST", routes.ResourceIndex.addResource().url())
      .bodyJson(likeAgent.toJson()));
    assertEquals(201, result.status());

    Resource likeAction = getResourceFromJsonFileUnsafe("ResourceIndexTest/testLikeAction.json");
    result = route(fakeRequest("POST", routes.ResourceIndex.addResource().url())
      .bodyJson(likeAction.toJson()));
    assertEquals(201, result.status());
  }

  private String getAuthString() {
    return Base64.getEncoder().encodeToString("user:pass".getBytes());
  }
}
