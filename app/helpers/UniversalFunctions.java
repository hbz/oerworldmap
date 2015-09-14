package helpers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
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

}
