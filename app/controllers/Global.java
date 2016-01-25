package controllers;

import com.typesafe.config.ConfigFactory;
import io.apigee.trireme.core.NodeEnvironment;
import io.apigee.trireme.core.NodeException;
import io.apigee.trireme.core.NodeScript;
import io.apigee.trireme.core.ScriptFuture;
import play.Application;
import play.Configuration;
import play.GlobalSettings;
import play.Logger;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class Global extends GlobalSettings {

  private static Configuration appConfig;

  private static ScriptFuture framer;

  @Override
  public void onStart(Application app) {

    if (app.isTest()){
      appConfig = new Configuration(ConfigFactory.parseFile(new File("conf/test.conf")).resolve());
    }
    else{
      appConfig = new Configuration(ConfigFactory.parseFile(new File("conf/application.conf")).resolve());
    }

    NodeEnvironment env = new NodeEnvironment();
    try {
      NodeScript script = env.createScript("frame.js",
        new File("node/json-frame/frame.js"), null);
      script.setNodeVersion("0.10");
      framer = script.executeModule();
      framer.getModuleResult();
    } catch (InterruptedException | ExecutionException | NodeException e) {
      Logger.error(e.toString());
    }

    Logger.info("oerworldmap has started");
    Locale.setDefault(new Locale("en", "US"));

  }

  @Override
  public void onStop(Application app) {
    framer.cancel(true);
    Logger.info("oerworldmap shutdown...");
  }

  public static Configuration getConfig() {
    return appConfig;
  }

}
