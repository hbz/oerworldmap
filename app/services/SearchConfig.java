package services;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigValue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author pvb
 */
public class SearchConfig {

  private Set<Entry<String, ConfigValue>> mEntries;
  private Config mConfig;
  private static final String DEFAULT_CONFIG_FILE = "conf/search.conf";

  public SearchConfig() {
    this(DEFAULT_CONFIG_FILE);
  }

  public SearchConfig(String aConfigFile) {
    File configFile = new File(aConfigFile);
    checkFileExists(configFile);
    mConfig = ConfigFactory.parseFile(configFile).resolve();
    mEntries = mConfig.entrySet();
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

  private Map<String, Double> getBoosts() {
    Map<String, Double> result = new HashMap<>();
    for (Entry<String, ConfigValue> entry : mEntries) {
      if (entry.getKey().startsWith("\"boost:")) {
        String key = entry.getKey().replaceAll("\"", "").substring(6);
        Double value = Double.valueOf((entry.getValue()).render(ConfigRenderOptions.defaults()));
        result.put(key, value);
      }
    }
    return result;
  }

  public String[] getBoostsForElasticsearch() {
    List<String> result = new ArrayList<>();
    Map<String, Double> boostMap = getBoosts();
    for (Map.Entry<String, Double> boost : boostMap.entrySet()) {
      result.add(boost.getKey() + "^" + boost.getValue());
    }
    return result.toArray(new String[0]);
  }
}
