package controllers;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import helpers.JsonLdConstants;
import org.junit.Test;
import play.mvc.Result;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;
import static play.test.Helpers.running;
import static play.test.Helpers.status;

/**
 * @author fo
 */
public class ResourceIndexTest {

  @Test
  public void postApplicationFormUrlEncoded() {
    running(fakeApplication(), new Runnable() {
      @Override
      public void run() {
        Map<String,String> data = new HashMap<>();
        data.put(JsonLdConstants.TYPE, "Person");
        data.put(JsonLdConstants.ID, UUID.randomUUID().toString());
        data.put("email", "foo@bar.com");
        Result result = route(fakeRequest("POST", routes.ResourceIndex.create().url()).withFormUrlEncodedBody(data));
        assertEquals(201, status(result));
      }
    });
  }

  @Test
  public void postApplicationJson() {
    running(fakeApplication(), new Runnable() {
      @Override
      public void run() {
        ObjectNode data = new ObjectNode(JsonNodeFactory.instance);
        data.put(JsonLdConstants.TYPE, "Person");
        data.put(JsonLdConstants.ID, UUID.randomUUID().toString());
        data.put("email", "foo@bar.com");
        Result result = route(fakeRequest("POST", routes.ResourceIndex.create().url()).withJsonBody(data));
        assertEquals(201, status(result));
      }
    });
  }

}
