package helpers;

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
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.Z");
    df.setTimeZone(tz);
    return df.format(new Date());
  }
}
