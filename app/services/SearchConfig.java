package services;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigValue;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author pvb
 */
public class SearchConfig {

  private Config mConfig = null;
  private final List<String> mBoosts = new ArrayList<>();
  private static final String DEFAULT_CONFIG_FILE = "conf/search.conf";
  private static Config mDefaultConfig = ConfigFactory.parseFile(new File(DEFAULT_CONFIG_FILE)).resolve();
  private static final List<String> mDefaultBoosts = new ArrayList<>();

  public SearchConfig(){
    if (mDefaultBoosts.size() == 0) {
      calculateBoostFromConfig(mDefaultBoosts, mDefaultConfig);
    }
  }

  public SearchConfig(@Nonnull String aConfigFile) {
    File configFile = new File(aConfigFile);
    checkFileExists(configFile);
    mConfig = ConfigFactory.parseFile(configFile).resolve();
    calculateBoostFromConfig(mBoosts, mConfig);
  }

  private void checkFileExists(File aFile) {
    if (!aFile.exists()) {
      try {
        throw new java.io.FileNotFoundException("Search config file \"" + aFile.getAbsolutePath() + "\" not found.");
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    }
  }

  private Map<String, Double> getBoosts(Config aConfig) {
    Map<String, Double> result = new HashMap<>();
    for (Entry<String, ConfigValue> entry : aConfig.entrySet()) {
      if (entry.getKey().startsWith("\"boost:")) {
        String key = entry.getKey().replaceAll("\"", "").substring(6);
        Double value = Double.valueOf((entry.getValue()).render(ConfigRenderOptions.defaults()));
        result.put(key, value);
      }
    }
    return result;
  }

  public void calculateBoostFromConfig() {
    if (null != mConfig) {
      calculateBoostFromConfig(mBoosts, mConfig);
    }
    else {
      calculateBoostFromConfig(mDefaultBoosts, mDefaultConfig);
    }
  }

  private void calculateBoostFromConfig(List<String> aBoosts, Config aConfig) {
    aBoosts.clear();
    Map<String, Double> boostMap = getBoosts(aConfig);
    for (Map.Entry<String, Double> boost : boostMap.entrySet()) {
      aBoosts.add(boost.getKey() + "^" + boost.getValue());
    }
  }

  public String[] getBoostsForElasticsearch(){
    if (!mBoosts.isEmpty()) {
      return mBoosts.toArray(new String[0]);
    }
    return mDefaultBoosts.toArray(new String[0]);
  }

}
