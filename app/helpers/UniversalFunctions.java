package helpers;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;

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
}
