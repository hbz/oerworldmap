package controllers;

import static org.junit.Assert.assertEquals;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;
import static play.test.Helpers.running;
import static play.test.Helpers.status;

import helpers.JsonLdConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import play.mvc.Result;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author fo
 */
public class ResourceIndexTest {

  /*private static Map<String, Object> testParameters = new HashMap<String, Object>();

  @SuppressWarnings("unchecked")
  @Before
  public void initialize() {
    Configuration testConfiguration;
    Config additionalConfig = ConfigFactory.parseFile(new File("conf/application.conf"));
    testConfiguration = new Configuration(additionalConfig);
    testParameters = testConfiguration.asMap();
    ((HashMap<String, Object>)((HashMap<String, Object>)((HashMap<String, Object>)testParameters.get("es")).get("index")).get("app")).put("name", "testindex");
    int i=0;
  }*/

  @Test
  public void createResourceFromFormUrlEncoded() {
    running(fakeApplication(), new Runnable() {
      @Override
      public void run() {
        Map<String, String> data = new HashMap<>();
        data.put(JsonLdConstants.TYPE, "Person");
        data.put(JsonLdConstants.ID, UUID.randomUUID().toString());
        data.put("email", "foo1@bar.com");
        Result result = route(fakeRequest("POST", routes.ResourceIndex.create().url())
            .withFormUrlEncodedBody(data));
        assertEquals(201, status(result));
      }
    });
  }

  @Test
  public void createResourceFromJson() {
    running(fakeApplication(), new Runnable() {
      @Override
      public void run() {
        ObjectNode data = new ObjectNode(JsonNodeFactory.instance);
        data.put(JsonLdConstants.TYPE, "Person");
        data.put(JsonLdConstants.ID, UUID.randomUUID().toString());
        data.put("email", "foo2@bar.com");
        Result result = route(fakeRequest("POST", routes.ResourceIndex.create().url())
            .withJsonBody(data));
        assertEquals(201, status(result));
      }
    });
  }

  @Test
  public void updateResourceFromJson() {
    running(fakeApplication(), new Runnable() {
      @Override
      public void run() {
        String id = UUID.randomUUID().toString();
        ObjectNode data = new ObjectNode(JsonNodeFactory.instance);
        data.put(JsonLdConstants.TYPE, "Person");
        data.put(JsonLdConstants.ID, id);
        data.put("email", "foo2@bar.com");
        Result createResult = route(fakeRequest("POST", routes.ResourceIndex.create().url())
            .withJsonBody(data));
        assertEquals(201, status(createResult));
        Result updateResult = route(fakeRequest("POST", routes.ResourceIndex.update(id).url())
            .withJsonBody(data));
        assertEquals(201, status(updateResult));
      }
    });
  }

  @Test
  public void updateNonexistentResourceFromJson() {
    running(fakeApplication(), new Runnable() {
      @Override
      public void run() {
        String id = UUID.randomUUID().toString();
        ObjectNode data = new ObjectNode(JsonNodeFactory.instance);
        data.put(JsonLdConstants.TYPE, "Person");
        data.put(JsonLdConstants.ID, id);
        data.put("email", "foo2@bar.com");
        Result updateResult = route(fakeRequest("POST", routes.ResourceIndex.update(id).url())
            .withJsonBody(data));
        assertEquals(400, status(updateResult));
      }
    });
  }

}
