package services;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigValue;
import helpers.FileHelpers;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import play.Logger;

/**
 * @author pvb
 */
public abstract class AbstractConfig {

  protected Set<Map.Entry<String, ConfigValue>> mEntries;
  protected Config mConfig;

  public AbstractConfig(String aConfigFile, String aFileType) {
    File configFile = new File(aConfigFile);
    try {
      FileHelpers.checkFileExists(configFile, aFileType);
    } catch (FileNotFoundException e) {
      Logger.error("Could not load file", e);
    }
    mConfig = ConfigFactory.parseFile(configFile).resolve();
    mEntries = mConfig.entrySet();
  }

  private Map<String, Double> getBoosts() {
    Map<String, Double> result = new HashMap<>();
    for (Map.Entry<String, ConfigValue> entry : mEntries) {
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
