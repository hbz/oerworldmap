package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import helpers.ElasticsearchTestGrid;
import helpers.JsonTest;
import models.Resource;
import models.TripleCommit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import play.Application;
import play.Configuration;
import play.Mode;
import play.api.test.FakeApplication;
import play.inject.guice.GuiceApplicationBuilder;
import play.libs.Json;
import play.test.Helpers;
import services.QueryContext;
import services.ReconcileConfig;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author pvb
 */
public class ReconcilerTest extends ElasticsearchTestGrid implements JsonTest {

  private static Reconciler mReconciler;
  private static Map<String, Object> mMetadata = new HashMap<>();
  private static QueryContext mDefaultQueryContext;
  protected Application application;

  @BeforeClass
  public static void setUp() throws IOException {
    mReconciler = new Reconciler(new Configuration(mConfig), null);
    mMetadata.put(TripleCommit.Header.AUTHOR_HEADER, "Anonymous");
    mMetadata.put(TripleCommit.Header.DATE_HEADER, "2016-04-08T17:34:37.038+02:00");
    mDefaultQueryContext = new QueryContext(null, null);
    mDefaultQueryContext.setElasticsearchFieldBoosts(new ReconcileConfig().getBoostsForElasticsearch());
  }

  @Before
  public void startApp() throws Exception {
    ClassLoader classLoader = FakeApplication.class.getClassLoader();
    application = new GuiceApplicationBuilder().in(classLoader)
      .in(Mode.TEST).build();
    Helpers.start(application);
  }

  @Test
  public void testReconcileBasic() throws IOException {
    final String correctTitle = "MyCorrectResourceTitle";
    Resource resource1 = getResourceFromJsonFile("ReconcilerTest/testReconcileBasic.IN.1.json");
    Resource resource2 = getResourceFromJsonFile("ReconcilerTest/testReconcileBasic.IN.2.json");
    mReconciler.getBaseRepository().addItem(resource1, mMetadata);
    mReconciler.getBaseRepository().addItem(resource2, mMetadata);

    // build query
    final Map<String, JsonNode> queryMap = new HashMap<>();
    final ObjectNode query0 = Json.newObject();
    query0.put("query", correctTitle);
    query0.put("limit", 1);
    queryMap.put("q0", query0);

    final JsonNode myResourceTitle = mReconciler.reconcile(queryMap.entrySet().iterator(), mDefaultQueryContext);
    Assert.assertEquals(correctTitle, myResourceTitle.get("q0").get("result").get(0).get("name").asText());
  }

}
