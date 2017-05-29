package controllers;

import com.typesafe.config.ConfigFactory;
import helpers.ElasticsearchTestGrid;
import helpers.JsonLdConstants;
import helpers.JsonTest;
import models.Resource;
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

  // @Test
  public void createResourceFromFormUrlEncoded() {

    Map<String, String> data = new HashMap<>();
    data.put(JsonLdConstants.TYPE, "Organization");
    data.put(JsonLdConstants.ID, "info:urn:uuid:" + UUID.randomUUID().toString());
    data.put("email", "foo1@bar.com");
    data.put("name[0][@value]", "Foo");
    data.put("name[0][@language]", "en");
    data.put("description[0][@value]", "Foo");
    data.put("description[0][@language]", "en");
    data.put("location[address][addressCountry]", "DE");
    Result result = route(fakeRequest("POST", routes.ResourceIndex.addResource().url())
      .bodyForm(data));
    assertEquals(201, result.status());

  }


  // @Test
  public void createResourceFromJson() {

    Resource event = getResourceFromJsonFileUnsafe("ResourceIndexTest/testEvent.json");
    Result result = route(fakeRequest("POST", routes.ResourceIndex.addResource().url())
      .bodyJson(event.toJson()));
    assertEquals(201, result.status());

  }

  // @Test
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

  // @Test
  public void updateNonexistentResourceFromJson() {

    Resource organization = getResourceFromJsonFileUnsafe("SchemaTest/testOrganization.json");
    String auth = getAuthString();
    Result updateResult = route(fakeRequest("POST", routes.ResourceIndex.updateResource(organization.getId()).url())
      .header("Authorization", "Basic " + auth).bodyJson(organization.toJson()));
    assertEquals(404, updateResult.status());

  }

  private String getAuthString() {
    Configuration conf = new Configuration(ConfigFactory.parseFile(new File("conf/test.conf")).resolve());
    String email = conf.getString("admin.user");
    String pass = conf.getString("admin.pass");
    String authString = email.concat(":").concat(pass);
    return Base64.getEncoder().encodeToString(authString.getBytes());
  }

}
