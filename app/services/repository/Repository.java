package services.repository;

import com.typesafe.config.Config;

/**
 * @author fo
 */
public abstract class Repository {

  Config mConfiguration;

  public Repository(Config aConfiguration) {
    this.mConfiguration = aConfiguration;
  }
}
