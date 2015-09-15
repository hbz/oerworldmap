package controllers;

import static org.junit.Assert.assertEquals;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;
import static play.test.Helpers.running;
import static play.test.Helpers.status;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import helpers.JsonLdConstants;

import java.io.File;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import models.Resource;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import play.mvc.Result;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import services.ElasticsearchConfig;
import services.ElasticsearchProvider;

/**
 * @author fo
 */
public class ResourceIndexTest {

  private static Config mConfig;
  private static ElasticsearchProvider mEsClient;

  @BeforeClass
  public static void setup() {
    mConfig = ConfigFactory.parseFile(new File("conf/test.conf")).resolve();
    ElasticsearchConfig elasticsearchConfig = new ElasticsearchConfig(mConfig);
    Settings settings = ImmutableSettings.settingsBuilder()
        .put(elasticsearchConfig.getClientSettings()).build();
    Client client = new TransportClient(settings)
        .addTransportAddress(new InetSocketTransportAddress(elasticsearchConfig.getServer(), 9300));
    mEsClient = new ElasticsearchProvider(client, elasticsearchConfig);
    mEsClient.createIndex(mConfig.getString("es.index.name"));
  }

  @Test
  public void createResourceFromFormUrlEncoded() {
    final Resource user = new Resource("Person");
    final String email = "foo@bar.de";
    user.put("email", email);
    running(fakeApplication(), new Runnable() {
      @Override
      public void run() {
        // String token = Account.createTokenFor(user);
        // String authString = email + ":" + token;
        String authString = "user:pass";
        String auth = Base64.getEncoder().encodeToString(authString.getBytes());
        Map<String, String> data = new HashMap<>();
        data.put(JsonLdConstants.TYPE, "Person");
        data.put(JsonLdConstants.ID, UUID.randomUUID().toString());
        data.put("email", "foo1@bar.com");
        Result result = route(fakeRequest("POST", routes.ResourceIndex.create().url()).withHeader(
            "Authorization", "Basic " + auth).withFormUrlEncodedBody(data));
        assertEquals(201, status(result));
      }
    });
  }

  @Test
  public void createResourceFromJson() {
    final Resource user = new Resource("Person");
    final String email = "foo@bar.de";
    user.put("email", email);
    running(fakeApplication(), new Runnable() {
      @Override
      public void run() {
        // String token = Account.createTokenFor(user);
        // String authString = email + ":" + token;
        String authString = "user:pass";
        String auth = Base64.getEncoder().encodeToString(authString.getBytes());
        ObjectNode data = new ObjectNode(JsonNodeFactory.instance);
        data.put(JsonLdConstants.TYPE, "Person");
        data.put(JsonLdConstants.ID, UUID.randomUUID().toString());
        data.put("email", "foo2@bar.com");
        Result result = route(fakeRequest("POST", routes.ResourceIndex.create().url()).withHeader(
            "Authorization", "Basic " + auth).withJsonBody(data));
        assertEquals(201, status(result));
      }
    });
  }

  @Test
  public void updateResourceFromJson() {
    final Resource user = new Resource("Person");
    final String email = "foo@bar.de";
    user.put("email", email);
    running(fakeApplication(), new Runnable() {
      @Override
      public void run() {
        // String token = Account.createTokenFor(user);
        // String authString = email + ":" + token;
        String authString = "user:pass";
        String auth = Base64.getEncoder().encodeToString(authString.getBytes());
        String id = UUID.randomUUID().toString();
        ObjectNode data = new ObjectNode(JsonNodeFactory.instance);
        data.put(JsonLdConstants.TYPE, "Person");
        data.put(JsonLdConstants.ID, id);
        data.put("email", "foo2@bar.com");
        Result createResult = route(fakeRequest("POST", routes.ResourceIndex.create().url())
            .withHeader("Authorization", "Basic " + auth).withJsonBody(data));
        assertEquals(201, status(createResult));
        Result updateResult = route(fakeRequest("POST", routes.ResourceIndex.update(id).url())
            .withHeader("Authorization", "Basic " + auth).withJsonBody(data));
        assertEquals(201, status(updateResult));
      }
    });
  }

  @Test
  public void updateNonexistentResourceFromJson() {
    final Resource user = new Resource("Person");
    final String email = "foo@bar.de";
    user.put("email", email);
    running(fakeApplication(), new Runnable() {
      @Override
      public void run() {
        // String token = Account.createTokenFor(user);
        // String authString = email + ":" + token;
        String authString = "user:pass";
        String auth = Base64.getEncoder().encodeToString(authString.getBytes());
        String id = UUID.randomUUID().toString();
        ObjectNode data = new ObjectNode(JsonNodeFactory.instance);
        data.put(JsonLdConstants.TYPE, "Person");
        data.put(JsonLdConstants.ID, id);
        data.put("email", "foo2@bar.com");
        Result updateResult = route(fakeRequest("POST", routes.ResourceIndex.update(id).url())
            .withHeader("Authorization", "Basic " + auth).withJsonBody(data));
        assertEquals(400, status(updateResult));
      }
    });
  }

  @AfterClass
  public static void tearDown() {
    mEsClient.deleteIndex(mConfig.getString("es.index.name"));
  }

}
