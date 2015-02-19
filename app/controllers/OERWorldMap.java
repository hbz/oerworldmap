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
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.util.ResourceBundle;


/**
 * @author fo
 */
public abstract class OERWorldMap extends Controller {
    
  final protected static Configuration mConf = Play.application().configuration();
    
  final private static Settings clientSettings = ImmutableSettings.settingsBuilder()
        .put(new ElasticsearchConfig().getClientSettings()).build();
  final private static Client mClient = new TransportClient(clientSettings)
        .addTransportAddress(new InetSocketTransportAddress(new ElasticsearchConfig().getServer(),
            9300));
  final private static ElasticsearchClient mElasticsearchClient = new ElasticsearchClient(mClient);
  final protected static ElasticsearchRepository resourceRepository = new ElasticsearchRepository(mElasticsearchClient);

  final protected static FileResourceRepository mUnconfirmedUserRepository;
  static {
    try {
      mUnconfirmedUserRepository = new FileResourceRepository(Paths.get(mConf.getString("filerepo.dir")));
    } catch(final Exception ex) {
      throw new RuntimeException("Failed to create FileResourceRespository", ex);
    }
  }

  // Internationalization
  protected static Locale currentLocale;
  static {
    try {
      currentLocale = request().acceptLanguages().get(0).toLocale();
    } catch (IndexOutOfBoundsException e) {
      currentLocale = Locale.getDefault();
    }
  }
  
  protected static Html render(String pageTitle, Map<String, Object> data, String templatePath) {

    // Internationalization
    Locale currentLocale;
    try {
      currentLocale = request().acceptLanguages().get(0).toLocale();
    } catch (IndexOutOfBoundsException e) {
      currentLocale = Locale.getDefault();
    }

    ResourceBundle messages = ResourceBundle.getBundle("MessagesBundle", currentLocale);
    Map<String, String> i18n = new HashMap<>();
    for (String key : Collections.list(messages.getKeys())) {
      try {
        i18n.put(key, new String(messages.getString(key).getBytes("ISO-8859-1"), "UTF-8"));
      } catch (UnsupportedEncodingException e) {
        i18n.put(key, messages.getString(key));
      }
    }
    data.put("i18n", i18n);
    
    Mustache template = MustacheFactory.compile(templatePath);
    Writer writer = new StringWriter();
    template.execute(writer, data);
    return views.html.main.render(pageTitle, Html.apply(writer.toString()));

  }
  
}
