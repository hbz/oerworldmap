package services;

/**
 * @author pvb
 */
public class ReconcileConfig extends AbstractConfig {

  private static final String DEFAULT_CONFIG_FILE = "conf/reconcile.conf";

  public ReconcileConfig() {
    this(DEFAULT_CONFIG_FILE);
  }

  public ReconcileConfig(String aConfigFile) {
    super(aConfigFile, "Reconcile configuration");
  }

}
