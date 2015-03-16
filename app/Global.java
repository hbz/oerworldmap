import play.Application;
import play.GlobalSettings;
import play.Logger;
import services.ElasticsearchConfig;

import java.util.Locale;

public class Global extends GlobalSettings {

  private static ElasticsearchConfig esConfig = new ElasticsearchConfig(false);

  @Override
  public void onStart(Application app) {
    Logger.info("oerworldmap has started");
    Logger.info("Elasticsearch config: " + esConfig.toString());
    Locale.setDefault(new Locale("en", "US"));
  }

  @Override
  public void onStop(Application app) {
    Logger.info("oerworldmap shutdown...");
  }

}
