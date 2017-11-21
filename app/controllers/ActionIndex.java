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
  protected Result upsertItem(boolean aBoolean) throws IOException {
    return null; // TODO
  }

  @Override
  protected Result upsertItems() throws IOException {
    return null; // TODO
  }

  @Override
  public Result read(String id, String version, String extension) throws IOException {
    return null; // TODO
  }
}
