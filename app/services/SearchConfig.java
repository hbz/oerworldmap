package services;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigValue;

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
    File configFile;
    if (!StringUtils.isEmpty(aConfigFile)) {
      configFile = new File(aConfigFile);
    } else {
      configFile = new File(DEFAULT_CONFIG_FILE);
    }
    init(configFile);
  }

  private void init(File aConfigFile) {
    // CONFIG FILE
    checkFileExists(aConfigFile);
    mConfig = ConfigFactory.parseFile(aConfigFile).resolve();
    init();
  }

  private void init() {
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

  public Map<String, Double> getBoosts() {
    Map<String, Double> result = new HashMap<>();
    for (Entry<String, ConfigValue> entry : mEntries) {
      if (entry.getKey().startsWith("\"boost:")) {
        String key = entry.getKey().replaceAll("\"", "").substring(6);
        Double value = Double.valueOf(((ConfigValue) entry.getValue()).render(ConfigRenderOptions.defaults()));
        result.put(key, value);
      }
    }
    return result;
  }

  public String[] getBoostsForElasticsearch() {
    List<String> result = new ArrayList<>();
    Map<String, Double> boostMap = getBoosts();
    for (Map.Entry<String, Double> boost : boostMap.entrySet()) {
      StringBuilder esboost = new StringBuilder(boost.getKey());
      esboost.append("^").append(boost.getValue());
      result.add(esboost.toString());
    }
    return result.toArray(new String[0]);
  }
}
