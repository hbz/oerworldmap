package controllers;

import com.typesafe.config.ConfigFactory;
import play.Application;
import play.Configuration;
import play.GlobalSettings;
import play.Logger;

import java.io.File;
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

  public static Configuration getConfig() {
    return appConfig;
  }

}
