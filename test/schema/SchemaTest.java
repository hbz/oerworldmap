package schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.processors.syntax.SyntaxValidator;
import helpers.FilesConfig;
import helpers.JsonTest;
import models.Resource;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertTrue;


/**
 * @author fo
 */
public class SchemaTest implements JsonTest {

  private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  public void testSchema() throws IOException {
    SyntaxValidator syntaxValidator = JsonSchemaFactory.byDefault().getSyntaxValidator();
    JsonNode schema = OBJECT_MAPPER.readTree(Paths.get(FilesConfig.getResourceSchema()).toFile());
    assertTrue(syntaxValidator.schemaIsValid(schema));
  }

  @Test
  public void testInstances() throws IOException {
    List<Resource> resources = getResourcesFromJsonDir("SchemaTest/");
    for (Resource resource : resources) {
      ProcessingReport processingReport = resource.validate();
      assertTrue(processingReport.toString().concat(resource.toString()), processingReport.isSuccess());
    }
  }

}
