package helpers;

import org.apache.bcel.util.ClassLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by pvb on 14.10.16.
 */
public class FileHelpers {

  public static BufferedReader getBufferedReaderFrom(String aFile) throws IOException {
    InputStream in = ClassLoader.getSystemResourceAsStream(aFile);
    if (in.available() < 1){
      return null;
    }
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    return reader;
  }

}
