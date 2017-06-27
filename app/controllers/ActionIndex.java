package controllers;

import play.Configuration;
import play.Environment;
import play.mvc.Result;

import java.io.IOException;

/**
 * @author pvb
 */
public class ActionIndex extends IndexCommon {
  public ActionIndex(Configuration aConf, Environment aEnv) {
    super(aConf, aEnv);
  }

  @Override
  Result upsertResource(boolean aBoolean) throws IOException {
    return null;
  }

  @Override
  Result upsertResources() throws IOException {
    return null;
  }
}
