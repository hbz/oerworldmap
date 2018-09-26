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
import services.SearchConfig;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author pvb
 */
public class ReconcilerTest extends ElasticsearchTestGrid implements JsonTest {

  private static Reconciler mReconciler;
  private static Map<String, String> mMetadata = new HashMap<>();
  private static QueryContext mDefaultQueryContext;
  protected Application application;

  @BeforeClass
  public static void setUp() throws IOException {
    mReconciler = new Reconciler(new Configuration(mConfig), null);
    mMetadata.put(TripleCommit.Header.AUTHOR_HEADER, "Anonymous");
    mMetadata.put(TripleCommit.Header.DATE_HEADER, "2016-04-08T17:34:37.038+02:00");
    mDefaultQueryContext = new QueryContext(null);
    mDefaultQueryContext
      .setElasticsearchFieldBoosts(new ReconcileConfig().getBoostsForElasticsearch());
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
    mReconciler.getBaseRepository().addResource(resource1, mMetadata);
    mReconciler.getBaseRepository().addResource(resource2, mMetadata);

    // build query
    final Map<String, JsonNode> queryMap = new HashMap<>();
    final ObjectNode query0 = Json.newObject();
    query0.put("query", correctTitle);
    query0.put("limit", 1);
    queryMap.put("q0", query0);

    final JsonNode myResourceTitle =
      mReconciler.reconcile(queryMap.entrySet().iterator(), mDefaultQueryContext, Locale.GERMAN);
    Assert.assertEquals(correctTitle,
      myResourceTitle.get("q0").get("result").get(0).get("name").asText());
  }

  @Test
  public void testFuzzyTokenSearch() throws IOException {
    Resource db1 = getResourceFromJsonFile("ReconcilerTest/testFuzzyTokenSearch.DB.1.json"); // TODO
    mReconciler.getBaseRepository().addResource(db1, mMetadata);

    final String withoutTypo = "Politischebildung";
    final String withTypo = "Poltischebildung";

    // build query
    final Map<String, JsonNode> queryMap = new HashMap<>();
    final ObjectNode query0 = Json.newObject();
    query0.put("limit", 1);

    try {
      query0.put("query", withoutTypo);
      queryMap.put("q0", query0);
      final JsonNode noTypoSearchNode =
        mReconciler.reconcile(queryMap.entrySet().iterator(), mDefaultQueryContext, Locale.GERMAN);
      Assert.assertEquals(withoutTypo,
        noTypoSearchNode.get("q0").get("result").get(0).get("name").asText());

      query0.put("query", withTypo);
      queryMap.put("q0", query0);
      final JsonNode typoSearchNode =
        mReconciler.reconcile(queryMap.entrySet().iterator(), mDefaultQueryContext, Locale.GERMAN);
      Assert.assertEquals(withoutTypo,
        typoSearchNode.get("q0").get("result").get(0).get("name").asText());
    } finally {
      mReconciler.getBaseRepository()
        .deleteResource("urn:uuid:a1e68ea9-4fc7-4c4a-be87-2065d070ab16", mMetadata);
    }
  }

  @Test
  public void testFuzzyPhraseSearch() throws IOException {
    Resource db1 = getResourceFromJsonFile(
      "ReconcilerTest/testFuzzyPhraseSearch.DB.1.json"); // TODO
    mReconciler.getBaseRepository().addResource(db1, mMetadata);

    final String withoutTypo = "Bundeszentrale für politische Bildung";
    final String withTypo = "Bundeszentrale für poltische Bildung";

    // build query
    final Map<String, JsonNode> queryMap = new HashMap<>();
    final ObjectNode query0 = Json.newObject();
    query0.put("limit", 1);

    try {
      query0.put("query", withoutTypo);
      queryMap.put("q0", query0);
      final JsonNode noTypoSearchNode = mReconciler
        .reconcile(queryMap.entrySet().iterator(), mDefaultQueryContext, Locale.GERMAN);
      Assert.assertEquals(withoutTypo,
        noTypoSearchNode.get("q0").get("result").get(0).get("name").asText());

      query0.put("query", withTypo);
      queryMap.put("q0", query0);
      final JsonNode typoSearchNode =
        mReconciler.reconcile(queryMap.entrySet().iterator(), mDefaultQueryContext, Locale.GERMAN);
      Assert.assertEquals(withoutTypo,
        typoSearchNode.get("q0").get("result").get(0).get("name").asText());
    } finally {
      mReconciler.getBaseRepository()
        .deleteResource("urn:uuid:a1e68ea9-4fc7-4c4a-be87-2065d070ab16", mMetadata);
    }
  }

  //@Test
  public void testSearchSpecialCaseCase() throws IOException {
    Resource db1 = getResourceFromJsonFile("ReconcilerTest/testSearchSpecialCaseCase.DB.1.json");
    mReconciler.getBaseRepository().addResource(db1, mMetadata);
    QueryContext queryContext = new QueryContext(null);
    queryContext.setElasticsearchFieldBoosts(new SearchConfig().getBoostsForElasticsearch());
    try {
      JsonNode hitsTrivial = mReconciler.getBaseRepository()
        .reconcile("BC campus", 0, 10, null, null, queryContext, Locale.ENGLISH);
      Assert.assertEquals("Did not get expected number of hits (1) for trivial case.", 1,
        hitsTrivial.get("result").size());
      JsonNode hitsSpecial = mReconciler.getBaseRepository()
        .reconcile("Bc campus", 0, 10, null, null, queryContext, Locale.ENGLISH);
      Assert.assertEquals("Did not get expected number of hits (1) for special case.", 1,
        hitsSpecial.get("result").size());
    } finally {
      mReconciler.getBaseRepository()
        .deleteResource("urn:uuid:374cce8a-2fbc-11e5-a656-001999ac7927.json", mMetadata);
    }
  }

  @Test
  public void testSearchSpecialCharAnd() throws IOException {
    Resource db1 = getResourceFromJsonFile("ReconcilerTest/testSearchSpecialCharAnd.DB.1.json");
    mReconciler.getBaseRepository().addResource(db1, mMetadata);
    QueryContext queryContext = new QueryContext(null);
    queryContext.setElasticsearchFieldBoosts(new SearchConfig().getBoostsForElasticsearch());
    try {
      JsonNode hitsTrivial = mReconciler.getBaseRepository()
        .reconcile("Kwame Nkrumah University of Science Technology", 0, 10, null, null, queryContext, Locale.ENGLISH);
      Assert.assertEquals("Did not get expected number of hits (1) for search without special char.", 1,
        hitsTrivial.get("result").size());
      JsonNode hitsSpecial = mReconciler.getBaseRepository()
        .reconcile("Kwame Nkrumah University of Science & Technology", 0, 10, null, null, queryContext, Locale.ENGLISH);
      Assert.assertEquals("Did not get expected number of hits (1) for special char search.", 1,
        hitsSpecial.get("result").size());
    } finally {
      mReconciler.getBaseRepository()
        .deleteResource("urn:uuid:6eadceb1-ee44-4cbd-a524-7d492961ec8e", mMetadata);
    }
  }
}
