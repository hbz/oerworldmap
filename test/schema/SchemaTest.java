package schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.processors.syntax.SyntaxValidator;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import helpers.JsonSchemaValidator;
import helpers.JsonTest;
import models.Resource;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertTrue;


/**
 * @author fo
 */
public class SchemaTest implements JsonTest {

  private static Config mConf;
  private static JsonSchemaValidator mSchemaValidator;

  @BeforeClass
  public static void setup() throws IOException {
    mConf = ConfigFactory.parseFile(new File("conf/test.conf")).resolve();
    mSchemaValidator = new JsonSchemaValidator(Paths.get(mConf.getString("json.schema")).toFile());
  }

  @Test
  public void testSchema() throws IOException {
    SyntaxValidator syntaxValidator = JsonSchemaFactory.byDefault().getSyntaxValidator();
    JsonNode schema = new ObjectMapper().readTree(Paths.get(mConf.getString("json.schema")).toFile());
    assertTrue(syntaxValidator.schemaIsValid(schema));
  }

  @Test
  public void testInstances() throws IOException {
    List<Resource> resources = getResourcesFromJsonDir("SchemaTest/");
    for (Resource resource : resources) {
      ProcessingReport processingReport = mSchemaValidator.validate(resource);
      assertTrue(processingReport.toString().concat(resource.toString()), processingReport.isSuccess());
    }
  }

}
