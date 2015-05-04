package controllers;

import play.Application;
import play.GlobalSettings;
import play.Logger;
import services.ElasticsearchConfig;

import java.util.Locale;

public class Global extends GlobalSettings {

  private static ElasticsearchConfig esConfig;

  @Override
  public void onStart(Application app) {
    if (app.isTest()){
      esConfig = new ElasticsearchConfig("conf/test.conf");
    }
    else{
      esConfig = new ElasticsearchConfig("conf/application.conf");
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
  
  public static ElasticsearchConfig createElasticsearchConfig(boolean isTest){
    if (isTest){
      return new ElasticsearchConfig("conf/test.conf");
    }
    else{
      return new ElasticsearchConfig("conf/application.conf");
    }
  }

}
