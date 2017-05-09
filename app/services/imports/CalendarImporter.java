package services.imports;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import models.Resource;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.util.PythonInterpreter;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
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
    PythonInterpreter.initialize(System.getProperties(), System.getProperties(), new String[0]);
    PythonInterpreter interpreter = new PythonInterpreter();
    interpreter.exec("import sys");
    interpreter.exec("sys.path.append('".concat(PATH_TO_PYTHON_IMPORTER).concat("')"));
    interpreter.exec("sys.path.append('".concat(PATH_TO_PYTHON_LIBRARY).concat("')"));
    interpreter.set("__file__", PATH_TO_PYTHON_IMPORTER);
    interpreter.exec("sys.argv.append('".concat("import.iCal.import_ical").concat("')"));
    interpreter.exec("sys.argv.append('".concat("./import/iCal/import_ical.py").concat("')"));
    interpreter.exec("sys.argv.append('".concat(aUrl).concat("')"));
    interpreter.exec("sys.argv.append('".concat(aLanguage).concat("')"));
    interpreter.exec("sys.argv.append('".concat("./import/iCal/cache/temp.json").concat("')"));
    interpreter.exec("sys.argv.append('".concat(aApiKey).concat("')"));
    interpreter.execfile(PATH_AND_NAME_OF_PYTHON_IMPORTER);
    PyObject importIcalPy = interpreter.get("import_ical");
    PyObject pythonResult = importIcalPy.__call__(new PyString(aUrl), new PyString(aLanguage), new PyString(aApiKey));
    String jsonArray = (String) pythonResult.__tojava__(String.class);
    ObjectMapper mapper = new ObjectMapper();
    List<Resource> result = mapper.readValue(jsonArray, new TypeReference<List<Resource>>() {});
    return result;
  }
}
