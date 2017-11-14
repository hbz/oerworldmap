package services;

/**
 * @author pvb
 */
public class SearchConfig extends AbstractConfig {

  private static final String DEFAULT_CONFIG_FILE = "conf/search.conf";

  public SearchConfig() {
    this(DEFAULT_CONFIG_FILE);
  }

  public SearchConfig(String aConfigFile) {
    super(aConfigFile, "Search configuration");
  }
}
