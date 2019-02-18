package helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author pvb
 */
public class FileHelpers {

  public static BufferedReader getBufferedReaderFrom(String aFile) throws IOException {
    InputStream in = ClassLoader.getSystemResourceAsStream(aFile);
    if (in.available() < 1) {
      return null;
    }
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    return reader;
  }

  public static void checkFileExists(final File aFile, final String aFileType)
    throws FileNotFoundException {
    if (!aFile.exists()) {
      throw new java.io.FileNotFoundException(
        aFileType + " file \"" + aFile.getAbsolutePath() + "\" not found.");
    }
  }
}
