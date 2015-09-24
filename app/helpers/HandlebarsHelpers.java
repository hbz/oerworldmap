package helpers;

import java.io.IOException;
import java.util.Map;

import com.github.jknack.handlebars.Options;

import play.Logger;

/**
 * @author fo
 */
public class HandlebarsHelpers {

  public CharSequence ifIn(String filter, String value, Map<String, String[]> filters,
      Options options) {
    try {
      String[] values = filters.get(filter);
      if (!(null == values))
        for (String member : values) {
          if (member.equals(value)) {
            return options.fn();
          }
        }
      return options.inverse();
    } catch (IOException e) {
      Logger.error(e.toString());
      return "";
    }
  }

}
