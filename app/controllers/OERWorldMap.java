package controllers;

import helpers.FilesConfig;
import io.michaelallen.mustache.MustacheFactory;
import io.michaelallen.mustache.api.Mustache;

import org.apache.commons.lang3.StringEscapeUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import play.Configuration;
import play.Play;
import play.mvc.Controller;
import play.twirl.api.Html;
import services.BaseRepository;
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
  final private static Settings mClientSettings = ImmutableSettings.settingsBuilder()
        .put(new ElasticsearchConfig().getClientSettings()).build();
  final private static Client mClient = new TransportClient(mClientSettings)
        .addTransportAddress(new InetSocketTransportAddress(new ElasticsearchConfig().getServer(),
            9300));
  final private static ElasticsearchClient mElasticsearchClient = new ElasticsearchClient(mClient);
  protected static BaseRepository mBaseRepository = null;
  
  static {
    try {
      mBaseRepository = new BaseRepository(mElasticsearchClient, Paths.get(FilesConfig.getRepo()));
    } catch(final Exception ex) {
      throw new RuntimeException("Failed to create Respository", ex);
    }
  }

  // Internationalization
  protected static Locale currentLocale;
  static {
    if (mConf.getBoolean("i18n.enabled")) {
      try {
        currentLocale = request().acceptLanguages().get(0).toLocale();
      } catch (IndexOutOfBoundsException e) {
        currentLocale = Locale.getDefault();
      }
    } else {
      currentLocale = new Locale("en");
    }
  }

  protected static Map<String, String> i18n = new HashMap<>();
  static {
    ResourceBundle messages = ResourceBundle.getBundle("MessagesBundle", currentLocale);
    for (String key : Collections.list(messages.getKeys())) {
      try {
        String message = StringEscapeUtils.unescapeJava(
            new String(messages.getString(key).getBytes("ISO-8859-1"), "UTF-8"));
        i18n.put(key, message);
      } catch (UnsupportedEncodingException e) {
        i18n.put(key, messages.getString(key));
      }
    }
  }

  // Data for mustaches
  protected static Map<String, Object> mResponseData = new HashMap<>();

  protected static Html render(String pageTitle, String templatePath) {

    mResponseData.put("i18n", i18n);
    Mustache template = MustacheFactory.compile(templatePath);
    Writer writer = new StringWriter();
    template.execute(writer, mResponseData);
    mResponseData.clear();
    return views.html.main.render(pageTitle, Html.apply(writer.toString()));

  }

}
