import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.inMemoryDatabase;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.junit.Test;

import play.libs.F.Callback;
import play.test.TestBrowser;
import services.ElasticsearchConfig;
import services.ElasticsearchProvider;

public class ApplicationTest {
  @Test  
  public void runningLandingPage() {
    
    // Setup Elasticsearch
    Node server = ElasticsearchProvider.createServerNode(true);
    Client client = ElasticsearchProvider.getClient(server);
    ElasticsearchProvider.createIndex(client, new ElasticsearchConfig().getIndex());
    
    running(testServer(3333, fakeApplication(inMemoryDatabase())), HTMLUNIT,
        new Callback<TestBrowser>() {
          @Override
          public void invoke(TestBrowser browser) {
            browser.goTo("http://localhost:3333/user");
            assertThat(browser.pageSource().contains("Registration"));
          }
        });
  }

}
