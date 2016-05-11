package controllers;

import com.typesafe.config.ConfigFactory;
import play.Application;
import play.Configuration;
import play.GlobalSettings;
import play.Logger;
import play.mvc.Action;
import play.mvc.Http;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Locale;

public class Global extends GlobalSettings {

  private static Configuration appConfig;

  @Override
  public void onStart(Application app) {
    if (app.isTest()){
      appConfig = new Configuration(ConfigFactory.parseFile(new File("conf/test.conf")).resolve());
    }
    else{
      appConfig = new Configuration(ConfigFactory.parseFile(new File("conf/application.conf")).resolve());
    }
    Logger.info("oerworldmap has started");
    Locale.setDefault(new Locale("en", "US"));
  }

  @Override
  public void onStop(Application app) {
    Logger.info("oerworldmap shutdown...");
  }

  @Override
  public Action onRequest(Http.Request request, Method actionMethod) {
    if (appConfig.getBoolean("i18n.enabled")) {
      try {
        Locale.setDefault(request.acceptLanguages().get(0).toLocale());
      } catch (IndexOutOfBoundsException e) {
        Locale.setDefault(new Locale("en"));
        Logger.error(e.toString());
      }
    } else {
      Locale.setDefault(new Locale("en"));
    }
    Logger.info("Language is " + Locale.getDefault());
    return super.onRequest(request, actionMethod);
  }

  public static Configuration getConfig() {
    return appConfig;
  }

}
