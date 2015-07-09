package controllers;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import play.Application;
import play.GlobalSettings;
import play.Logger;
import services.ElasticsearchConfig;

import java.io.File;
import java.util.Locale;

public class Global extends GlobalSettings {

  private static ElasticsearchConfig esConfig;

  private static Config appConfig;

  @Override
  public void onStart(Application app) {
    if (app.isTest()){
      esConfig = new ElasticsearchConfig("conf/test.conf");
      appConfig = ConfigFactory.parseFile(new File("conf/test.conf")).resolve();
    }
    else{
      esConfig = new ElasticsearchConfig("conf/application.conf");
      appConfig = ConfigFactory.parseFile(new File("conf/application.conf")).resolve();
    }
    Logger.info("oerworldmap has started");
    Logger.info("Elasticsearch config: " + esConfig.toString());
    Locale.setDefault(new Locale("en", "US"));
  }

  @Override
  public void onStop(Application app) {
    Logger.info("oerworldmap shutdown...");
  }
  
  public static ElasticsearchConfig getElasticsearchConfig(){
    return esConfig;
  }

  public static Config getConfig() {
    return appConfig;
  }
  
  public static ElasticsearchConfig createElasticsearchConfig(boolean isTest){
    if (isTest){
      return new ElasticsearchConfig("conf/test.conf");
    }
    else{
      return new ElasticsearchConfig("conf/application.conf");
    }
  }

}
