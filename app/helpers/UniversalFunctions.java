package helpers;

import com.fasterxml.jackson.databind.JsonNode;
import models.ModelCommon;
import models.TripleCommit;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TimeZone;

public class UniversalFunctions {

  public static String readFile(String aPath, Charset aEncoding) throws IOException {
    return new String(Files.readAllBytes(Paths.get(aPath)), aEncoding);
  }

  public static String collectionToString(Collection<? extends Object> aCollection){
    String string = aCollection.getClass().getName() + ": {";
    for (Object entry : aCollection){
      string = string.concat("\n\t").concat(entry.toString());
    }
    return string.concat("\n}");
  }

  public static String getCurrentTime() {
    TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
    df.setTimeZone(tz);
    return df.format(new Date());
  }

  public static String getHtmlEntities(String aString) {
    if (null == aString) {
      return "";
    }
    String escapedString = "";
    for (int i = 0; i < aString.length(); i++) {
      char c = aString.charAt(i);
      int value = c;
      escapedString += "&#" + value + ";";
    }
    return escapedString;
  }

  public static boolean deleteDirectory(File path) {
    if (path != null && path.exists()) {
      for (File file : path.listFiles()){
        if (file.isDirectory()){
          deleteDirectory(file);
        }
        else{
          file.delete();
        }
      }
      return(path.delete());
    }
    return false;
  }

  public static Map<String, String> resourceBundleToMap(ResourceBundle aResourceBundle) {
    Map<String, String> map = new HashMap<>();
    Enumeration<String> keys = aResourceBundle.getKeys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement();
      map.put(key, aResourceBundle.getString(key));
    }
    return map;
  }

  public static TripleCommit.Header getTripleCommitHeaderFromMetadata(final Map<String, Object> aMetadata){
    return new TripleCommit.Header(aMetadata.get(TripleCommit.Header.AUTHOR_HEADER).toString(),
      ZonedDateTime.parse((CharSequence)aMetadata.get(TripleCommit.Header.DATE_HEADER)));
  }

  public static ModelCommon buildItemFromJson(final JsonNode aJsonNode, final Types aTypes){
    ModelCommon item = null;
    Class clazz = aTypes.getItemClass(aJsonNode);
    if (clazz == null) {
      throw new IllegalArgumentException("No class found for type of node:\n" + aJsonNode);
    }
    Constructor<? extends ModelCommon> constructor = null;
    try {
      constructor = (Constructor<? extends ModelCommon>) clazz.getConstructor(JsonNode.class);
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    }
    try {
      item = constructor.newInstance(aJsonNode);
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
    return item;
  }

}
