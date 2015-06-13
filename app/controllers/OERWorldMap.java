package controllers;

import helpers.FilesConfig;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;


import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import helpers.UniversalFunctions;
import org.apache.commons.lang3.StringEscapeUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import play.Configuration;
import play.Logger;
import play.Play;
import play.mvc.Controller;
import play.mvc.Http;
import play.twirl.api.Html;
import services.BaseRepository;
import services.ElasticsearchClient;
import services.ElasticsearchConfig;
import services.ElasticsearchRepository;

/**
 * @author fo
 */
public abstract class OERWorldMap extends Controller {

  final protected static Configuration mConf = Play.application().configuration();
  final private static ElasticsearchConfig mEsConfig = Global.getElasticsearchConfig();
  
  final private static Settings mClientSettings = ImmutableSettings.settingsBuilder()
      .put(mEsConfig.getClientSettings()).build();
  final private static Client mClient = new TransportClient(mClientSettings)
      .addTransportAddress(new InetSocketTransportAddress(mEsConfig.getServer(), 9300));
  // TODO final private static ElasticsearchClient mElasticsearchClient = new
  // ElasticsearchClient(mClient);
  protected static BaseRepository mBaseRepository = null;
  final private static ElasticsearchClient mElasticsearchClient = new ElasticsearchClient(mClient,
      mEsConfig);
  final protected static ElasticsearchRepository mResourceRepository = new ElasticsearchRepository(
      mElasticsearchClient);

  
  
  // TODO final protected static FileResourceRepository
  // mUnconfirmedUserRepository;
  static {
    try {
      mBaseRepository = new BaseRepository(mElasticsearchClient, Paths.get(FilesConfig.getRepo()));
    } catch (final Exception ex) {
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
        String message = StringEscapeUtils.unescapeJava(new String(messages.getString(key)
            .getBytes("ISO-8859-1"), "UTF-8"));
        i18n.put(key, message);
      } catch (UnsupportedEncodingException e) {
        i18n.put(key, messages.getString(key));
      }
    }
  }

  protected static Html render(String pageTitle, String templatePath, Map<String, Object> scope,
      List<Map<String, Object>> messages) {

    Map<String, Object> mustacheData = new HashMap<>();
    mustacheData.put("scope", scope);
    mustacheData.put("messages", messages);
    mustacheData.put("i18n", i18n);
    mustacheData.put("user", Secured.getHttpBasicAuthUser(Http.Context.current()));

    MustacheFactory mf = new DefaultMustacheFactory();
    Mustache template = mf.compile(play.Play.application().path().getAbsolutePath() + "/app/mustache/" + templatePath);
    Writer writer = new StringWriter();
    template.execute(writer, mustacheData);
    return views.html.main.render(pageTitle, Html.apply(writer.toString()), getClientTemplates());
  }

  protected static Html render(String pageTitle, String templatePath, Map<String, Object> scope) {
    return render(pageTitle, templatePath, scope, null);
  }

  protected static Html render(String pageTitle, String templatePath) {
    return render(pageTitle, templatePath, null, null);
  }

  private static String getClientTemplates() {
    List<String> templates = new ArrayList<>();
    try {
      Files.walk(Paths.get(play.Play.application().path().getAbsolutePath() + "/app/mustache/ClientTemplates"))
          .forEach(new Consumer<Path>() {
            @Override
            public void accept(Path path) {
              try {
                String template = "<script id=\"".concat(path.getFileName().toString())
                    .concat("\" type=\"text/mustache\">\n");
                template = template.concat(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
                template = template.concat("</script>\n\n");
                templates.add(template);
              } catch (IOException e) {
                Logger.error(e.toString());
              }
            }
          });
    } catch (IOException e) {
      Logger.error(e.toString());
    }
    return String.join("\n", templates);
  }

}
