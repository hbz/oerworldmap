package helpers;

import com.github.jknack.handlebars.io.URLTemplateLoader;

import java.io.IOException;
import java.net.URL;

/**
 * @author fo
 */
public class ResourceTemplateLoader extends URLTemplateLoader {

  private ClassLoader mClassLoader;

  public ResourceTemplateLoader(ClassLoader aClassLoader) {
    super();
    mClassLoader = aClassLoader;
  }

  protected URL getResource(final String location) throws IOException {
    return mClassLoader.getResource(location);
  }
}
