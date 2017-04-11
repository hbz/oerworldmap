package services.imports;

import com.fasterxml.jackson.databind.ObjectMapper;
import models.Resource;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.util.PythonInterpreter;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.Arrays;
import java.util.List;

/**
 * Created by pvb
 */

public class CalendarImporter implements Importer{

  final private static String PATH_TO_PYTHON_IMPORTER;
  static{
    FileSystem fileSystem = new File(".").toPath().getFileSystem();
    PATH_TO_PYTHON_IMPORTER = ".".concat(fileSystem.getSeparator()).concat("import").concat(fileSystem.getSeparator())
      .concat("iCal").concat(fileSystem.getSeparator()).concat("import_ical.py");
  }

  @Override
  public List<Resource> importFromUrl(String aUrl, String aLanguage) throws IOException {
    // TODO: how to get or determine mapzenApiKey ?
    String mapzenApiKey = "";
    return importFromUrl(aUrl, aLanguage, mapzenApiKey);
  }

  public static List<Resource> importFromUrl(String aUrl, String aLanguage, String aApiKey) throws IOException {
    PythonInterpreter interpreter = new PythonInterpreter();
    interpreter.exec("import ".concat(PATH_TO_PYTHON_IMPORTER));
    PyObject someFunc = interpreter.get("import_ical");
    PyObject pythonResult = someFunc.__call__(new PyString(aUrl), new PyString(aLanguage), new PyString(aApiKey));
    String jsonArray = (String) pythonResult.__tojava__(String.class);
    ObjectMapper mapper = new ObjectMapper();
    List<Resource> result = Arrays.asList(mapper.readValue(jsonArray, Resource[].class));
    return result;
  }
}
