package controllers;

import helpers.ElasticsearchTestGrid;
import helpers.JsonTest;
import models.Resource;
import models.TripleCommit;
import org.junit.BeforeClass;
import org.junit.Test;
import play.Configuration;
import play.mvc.Result;
import services.QueryContext;
import services.SearchConfig;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author pvb
 */
public class ReconcilerTest extends ElasticsearchTestGrid implements JsonTest {

  private static Reconciler mReconciler;
  private static Map<String, String> mMetadata = new HashMap<>();
  private static QueryContext mDefaultQueryContext;

  @BeforeClass
  public static void setUp() throws IOException {
    mReconciler = new Reconciler(new Configuration(mConfig), null);
    mMetadata.put(TripleCommit.Header.AUTHOR_HEADER, "Anonymous");
    mMetadata.put(TripleCommit.Header.DATE_HEADER, "2016-04-08T17:34:37.038+02:00");
    mDefaultQueryContext = new QueryContext(null);
    mDefaultQueryContext.setElasticsearchFieldBoosts(new SearchConfig("conf/reconcile.conf").getBoostsForElasticsearch());
  }

  @Test
  public void testReconcileBasic() throws IOException {
    Resource resource1 = getResourceFromJsonFile("ReconcilerTest/testReconcileBasic.IN.1.json");
    Resource resource2 = getResourceFromJsonFile("ReconcilerTest/testReconcileBasic.IN.2.json");
    mReconciler.getBaseRepository().addResource(resource1, mMetadata);
    mReconciler.getBaseRepository().addResource(resource2, mMetadata);
    final Result myResourceTitle = mReconciler.reconcile("MyCorrectResourceTitle", mDefaultQueryContext);

  }

}
