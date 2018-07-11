package services.repository;

import com.typesafe.config.Config;

/**
 * @author fo
 */
public abstract class Repository {

  protected Config mConfiguration;

  public Repository(Config aConfiguration) {
    this.mConfiguration = aConfiguration;
  }
}
