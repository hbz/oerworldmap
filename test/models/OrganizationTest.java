package models;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.report.ProcessingReport;

public class OrganizationTest {

  private static JsonSchema organizationSchema;
  private static JsonNode organizationInstance;

  @BeforeClass
  public static void testConstructorWithoutId() throws JsonProcessingException, ProcessingException,
      IOException {

    organizationSchema = JsonSchemaFactory.byDefault().getJsonSchema(
        new ObjectMapper().readTree(Paths.get("public/json/schema.json").toFile()));
    organizationInstance = new ObjectMapper().readTree(Files.readAllBytes(Paths
        .get("public/json/ld/organization.jsonld")));
  }

  @Test
  public void testValidate() throws ProcessingException {
    ProcessingReport report = organizationSchema.validate(organizationInstance);
    Assert.assertTrue(report.isSuccess());
  }
}
