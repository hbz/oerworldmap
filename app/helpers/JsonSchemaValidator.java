package helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import models.Resource;
import play.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Created by fo on 15.08.17.
 */
public class JsonSchemaValidator {

  private JsonNode mSchemaNode;

  public JsonSchemaValidator(File aSchemaFile) throws IOException {
    mSchemaNode = new ObjectMapper().readTree(aSchemaFile);
  }

  public ProcessingReport validate(Resource aResource) {
    ProcessingReport report = new ListProcessingReport();
    try {
      String type = aResource.getType();
      if (null == type) {
        report.error(new ProcessingMessage()
          .setMessage("No type found for ".concat(aResource.toString()).concat(", cannot validate")));
      } else {
        JsonSchema schema = JsonSchemaFactory.byDefault().getJsonSchema(mSchemaNode, "/definitions/".concat(type));
        report = schema.validate(aResource.toJson());
      }
    } catch (ProcessingException e) {
      Logger.error("Error validating", e);
    }
    return report;
  }

}
