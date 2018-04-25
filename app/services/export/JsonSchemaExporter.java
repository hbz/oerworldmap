package services.export;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.Record;
import models.Resource;
import models.ResourceList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by fo on 27.07.17.
 */
public class JsonSchemaExporter implements Exporter {

  @Override
  public String export(Resource aResource) {
    return export(new ResourceList(Arrays.asList(aResource), 0, "", 0, 0, null, null, null));
  }

  @Override
  public String export(ResourceList aResourceList) {

    ObjectNode schema = new ObjectNode(JsonNodeFactory.instance);
    ObjectNode properties = new ObjectNode(JsonNodeFactory.instance);
    ObjectNode idProperty = new ObjectNode(JsonNodeFactory.instance);
    ArrayNode items = new ArrayNode(JsonNodeFactory.instance);
    ArrayNode suggest = new ArrayNode(JsonNodeFactory.instance);

    for (Resource item: aResourceList.getItems()) {
      items.add(item.getAsResource(Record.RESOURCE_KEY).getId());
      suggest.add(item.getAsResource(Record.RESOURCE_KEY).toJson());
    }

    idProperty.put("type", "string");
    if (aResourceList.getItems().size() > 0) {
      idProperty.set("enum", items);
    } else {
      idProperty.put("pattern", "(?!)");
    }

    properties.set("@id", idProperty);

    schema.put("type", "object");
    schema.set("properties", properties);
    schema.set("_suggest", suggest);

    return schema.toString();

  }
}
