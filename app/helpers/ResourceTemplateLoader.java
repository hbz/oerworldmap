package helpers;

import com.github.jknack.handlebars.io.URLTemplateLoader;
import play.Play;

import java.io.IOException;
import java.net.URL;

/**
 * @author fo
 */
public class ResourceTemplateLoader extends URLTemplateLoader {
  protected URL getResource(final String location) throws IOException {
    return Play.application().classloader().getResource(location);
  }
}
