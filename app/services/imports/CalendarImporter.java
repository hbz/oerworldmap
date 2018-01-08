package services.imports;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import models.Resource;
import org.python.util.PythonInterpreter;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by pvb
 */

public class CalendarImporter implements Importer{

  final private Config mConfig;

  final private static String PATH_TO_PYTHON_LIBRARY;
  final private static String PATH_TO_PYTHON_IMPORTER;
  final private static String NAME_OF_PYTHON_IMPORTER;
  final private static String PATH_AND_NAME_OF_PYTHON_IMPORTER;

  static{
    FileSystem fileSystem = new File(".").toPath().getFileSystem();
    final String SEP = fileSystem.getSeparator();
    PATH_TO_PYTHON_LIBRARY = System.getProperty("user.home")
      .concat(SEP).concat(".local")
      .concat(SEP).concat("lib")
      .concat(SEP).concat("python2.7")
      .concat(SEP).concat("site-packages");
    PATH_TO_PYTHON_IMPORTER = new File(".").getAbsolutePath()
      .concat(SEP).concat("import")
      .concat(SEP).concat("iCal");
    NAME_OF_PYTHON_IMPORTER = "import_ical.py";
    PATH_AND_NAME_OF_PYTHON_IMPORTER = PATH_TO_PYTHON_IMPORTER
      .concat(SEP).concat(NAME_OF_PYTHON_IMPORTER);
  }

  public CalendarImporter(final Config aConfig){
    mConfig = aConfig;
  }

  @Override
  public List<Resource> importFromUrl(String aUrl, String aLanguage) throws IOException {
    String mapzenApiKey = mConfig.getString("mapzen.apikey");
    return importFromUrl(aUrl, aLanguage, mapzenApiKey);
  }

  private static List<Resource> importFromUrl(String aUrl, String aLanguage, String aApiKey) throws IOException {
    String[] arguments = {PATH_AND_NAME_OF_PYTHON_IMPORTER, "DUMMY", aUrl, aLanguage, "./import/iCal/cache/temp.json", aApiKey};
    PythonInterpreter.initialize(System.getProperties(), System.getProperties(), arguments);
    PythonInterpreter interpreter = new org.python.util.PythonInterpreter();
    interpreter.exec("import sys");
    interpreter.exec("sys.path.append('".concat(PATH_TO_PYTHON_IMPORTER).concat("')"));
    interpreter.exec("sys.path.append('".concat(PATH_TO_PYTHON_LIBRARY).concat("')"));
    interpreter.set("__file__", PATH_TO_PYTHON_IMPORTER);
    interpreter.setOut(new StringWriter());
    interpreter.execfile(PATH_AND_NAME_OF_PYTHON_IMPORTER);
    ObjectMapper mapper = new ObjectMapper();
    String json = new String(Files.readAllBytes(Paths.get("./import/iCal/cache/temp.json")), StandardCharsets.UTF_8);
    while (json.startsWith("[[") && json.endsWith("]]")){
      json = json.substring(1, json.length()-1);
    }
    List<Resource> result = mapper.readValue(json, new TypeReference<List<Resource>>() {});
    return result;
  }

}
