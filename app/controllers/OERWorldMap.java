package controllers;

import helpers.Countries;
import helpers.FilesConfig;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.IOUtils;
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

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

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
    Locale.setDefault(currentLocale);
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
    i18n.putAll(Countries.map(currentLocale));
  }

  protected static Html render(String pageTitle, String templatePath, Map<String, Object> scope,
      List<Map<String, Object>> messages) {

    Map<String, Object> mustacheData = new HashMap<>();
    mustacheData.put("scope", scope);
    mustacheData.put("messages", messages);
    mustacheData.put("i18n", i18n);
    mustacheData.put("user", Secured.getHttpBasicAuthUser(Http.Context.current()));

    ClassLoader classLoader = Play.application().classloader();
    Mustache.Compiler compiler = Mustache.compiler().withLoader(new Mustache.TemplateLoader() {
      @Override
      public Reader getTemplate(String templatePath) throws Exception {
        Logger.info("Attempting to load template " + templatePath);
        return new InputStreamReader(classLoader.getResourceAsStream("public/mustache/"
            + templatePath));
      }
    });

    Template template = compiler.defaultValue("").compile(
        new InputStreamReader(classLoader.getResourceAsStream("public/mustache/" + templatePath)));
    return views.html.main.render(pageTitle, Html.apply(template.execute(mustacheData)),
        getClientTemplates());

  }

  protected static Html render(String pageTitle, String templatePath, Map<String, Object> scope) {
    return render(pageTitle, templatePath, scope, null);
  }

  protected static Html render(String pageTitle, String templatePath) {
    return render(pageTitle, templatePath, null, null);
  }

  private static String getClientTemplates() {

    final List<String> templates = new ArrayList<>();
    final String dir = "public/mustache/ClientTemplates/";
    final ClassLoader classLoader = Play.application().classloader();

    String[] paths = new String[0];
    try {
      paths = getResourceListing(dir, classLoader);
    } catch (URISyntaxException | IOException e) {
      Logger.error(e.toString());
    }

    for (String path : paths) {
      try {
        String template = "<script id=\"".concat(path).concat("\" type=\"text/mustache\">\n");
        template = template.concat(IOUtils.toString(classLoader.getResourceAsStream(dir + path)));
        template = template.concat("</script>\n\n");
        templates.add(template);
      } catch (IOException e) {
        Logger.error(e.toString());
      }
    }

    return String.join("\n", templates);

  }

  /**
   * List directory contents for a resource folder. Not recursive. This is
   * basically a brute-force implementation. Works for regular files and also
   * JARs.
   *
   * Adapted from http://www.uofr.net/~greg/java/get-resource-listing.html
   *
   * @param path
   *          Should end with "/", but not start with one.
   * @return Just the name of each member item, not the full paths.
   * @throws URISyntaxException
   * @throws IOException
   */
  private static String[] getResourceListing(String path, ClassLoader classLoader)
      throws URISyntaxException, IOException {

    URL dirURL = classLoader.getResource(path);
    ;
    if (dirURL == null) {
      return new File(play.Play.application().path().getAbsolutePath().concat("/").concat(path))
          .list();
    } else if (dirURL.getProtocol().equals("jar")) {
      String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); // strip
                                                                                     // out
                                                                                     // only
                                                                                     // the
                                                                                     // JAR
                                                                                     // file
      JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
      Enumeration<JarEntry> entries = jar.entries(); // gives ALL entries in jar
      Set<String> result = new HashSet<String>(); // avoid duplicates in case it
                                                  // is a subdirectory
      while (entries.hasMoreElements()) {
        String name = entries.nextElement().getName();
        if (name.startsWith(path)) { // filter according to the path
          String entry = name.substring(path.length());
          int checkSubdir = entry.indexOf("/");
          if (checkSubdir >= 0) {
            // if it is a subdirectory, we just return the directory name
            entry = entry.substring(0, checkSubdir);
          }
          if (!entry.equals("")) {
            result.add(entry);
          }
        }
      }
      jar.close();
      return result.toArray(new String[result.size()]);
    }

    throw new UnsupportedOperationException("Cannot list files for URL " + dirURL);

  }

}
