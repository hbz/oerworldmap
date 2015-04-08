package oerworldmap;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.inMemoryDatabase;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.junit.Test;

import play.Configuration;
import play.Play;
import play.libs.F.Callback;
import play.test.TestBrowser;
import services.ElasticsearchClient;
import services.ElasticsearchConfig;
import services.ElasticsearchProvider;
import services.ElasticsearchRepository;

public class ApplicationTest {
  @Test  
  public void runningLandingPage() {
    
    final ElasticsearchConfig config = new ElasticsearchConfig(true);
    final Settings mClientSettings = ImmutableSettings.settingsBuilder()
          .put(config.getClientSettings()).build();
    @SuppressWarnings("resource")
    final Client mClient = new TransportClient(mClientSettings)
          .addTransportAddress(new InetSocketTransportAddress(config.getServer(),
              9300));

    ElasticsearchProvider.createIndex(mClient, config.getIndex());
    
    running(testServer(3333, fakeApplication(inMemoryDatabase())), HTMLUNIT,
        new Callback<TestBrowser>() {
          @Override
          public void invoke(TestBrowser browser) {
            browser.goTo("http://localhost:3333/user");
            assertThat(browser.pageSource().contains("Registration"));
          }
        });
    mClient.close();
  }
}
