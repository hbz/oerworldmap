package controllers;

import io.michaelallen.mustache.MustacheFactory;
import io.michaelallen.mustache.api.Mustache;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import play.Configuration;
import play.Play;
import play.mvc.Controller;
import play.twirl.api.Html;
import services.ElasticsearchClient;
import services.ElasticsearchConfig;
import services.ElasticsearchRepository;
import services.FileResourceRepository;

import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.Map;

/**
 * @author fo
 */
public class OERWorldMap extends Controller {
    
  protected static Configuration mConf = Play.application().configuration();
    
  private static Settings clientSettings = ImmutableSettings.settingsBuilder()
        .put(new ElasticsearchConfig().getClientSettings()).build();
  private static Client mClient = new TransportClient(clientSettings)
        .addTransportAddress(new InetSocketTransportAddress(new ElasticsearchConfig().getServer(),
            9300));
  private static ElasticsearchClient mElasticsearchClient = new ElasticsearchClient(mClient);
  protected static ElasticsearchRepository resourceRepository = new ElasticsearchRepository(mElasticsearchClient);

  protected static FileResourceRepository mUnconfirmedUserRepository;
  static {
    try {
      mUnconfirmedUserRepository = new FileResourceRepository(Paths.get(mConf.getString("filerepo.dir")));
    } catch(final Exception ex) {
      throw new RuntimeException("Failed to create FileResourceRespository", ex);
    }
  }
  
  protected static Html render(String pageTitle, Map data, String templatePath) {

    Mustache template = MustacheFactory.compile(templatePath);
    Writer writer = new StringWriter();
    template.execute(writer, data);
    return views.html.main.render(pageTitle, Html.apply(writer.toString()));
    
  }
  
}
