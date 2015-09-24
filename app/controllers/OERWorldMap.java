package controllers;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
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

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.StringHelpers;
import com.github.jknack.handlebars.io.TemplateLoader;

import helpers.Countries;
import helpers.ResourceTemplateLoader;
import helpers.UniversalFunctions;
import models.Resource;
import play.Configuration;
import play.Logger;
import play.Play;
import play.mvc.Controller;
import play.mvc.Http;
import play.twirl.api.Html;
import services.repository.BaseRepository;

/**
 * @author fo
 */
public abstract class OERWorldMap extends Controller {

  final protected static Configuration mConf = Play.application().configuration();
  protected static BaseRepository mBaseRepository = null;

  // TODO final protected static FileRepository
  // mUnconfirmedUserRepository;
  static {
    try {
      mBaseRepository = new BaseRepository(Global.getConfig());
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
        String message = StringEscapeUtils
            .unescapeJava(new String(messages.getString(key).getBytes("ISO-8859-1"), "UTF-8"));
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

    TemplateLoader loader = new ResourceTemplateLoader();
    loader.setPrefix("public/mustache");
    loader.setSuffix("");
    Handlebars handlebars = new Handlebars(loader);

    handlebars.registerHelpers(StringHelpers.class);

    handlebars.registerHelper("size", new Helper<ArrayList<?>>() {
      public CharSequence apply(ArrayList<?> list, Options options) {
        return Integer.toString(list.size());
      }
    });

    handlebars.registerHelper("obfuscate", new Helper<String>() {
      public CharSequence apply(String string, Options options) {
        return UniversalFunctions.getHtmlEntities(string);
      }
    });

    handlebars.registerHelper("@value", new Helper<Resource>() {
      public CharSequence apply(Resource resource, Options options) {
        if (resource.get("@language") != null
            && resource.get("@language").toString().equals(Locale.getDefault().getLanguage())) {
          return resource.get("@value").toString();
        } else {
          return "";
        }
      }
    });

    try {
      Template template = handlebars.compile(templatePath);
      return views.html.main.render(pageTitle, Html.apply(template.apply(mustacheData)),
          getClientTemplates());
    } catch (IOException e) {
      Logger.error(e.toString());
      return null;
    }

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
