package services.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.Record;
import models.Resource;
import models.ResourceList;

/**
 * Created by fo on 27.03.17.
 */
public class GeoJsonExporter implements Exporter {

  @Override
  public String export(Resource aResource) {
    JsonNode node = toGeoJson(aResource);
    return node == null ? null : node.toString();
  }

  @Override
  public String export(ResourceList aResourceList) {
    ObjectNode node = new ObjectNode(JsonNodeFactory.instance);
    ArrayNode features = new ArrayNode(JsonNodeFactory.instance);
    node.put("type", "FeatureCollection");
    for (Resource resource : aResourceList.getItems()) {
      JsonNode feature = toGeoJson(resource);
      if (feature != null) {
        features.add(feature);
      }
    }
    node.set("features", features);
    return node.toString();
  }

  private JsonNode toGeoJson(Resource aResource) {

    Resource resource = aResource.getAsResource(Record.RESOURCE_KEY);
    Resource location = resource.getAsResource("location");
    if (location == null || location.getAsResource("geo") == null) {
      String[] traverse = new String[] {"mentions", "member", "agent", "participant", "provider"};
      for (String property : traverse) {
        Resource ref = aResource.getAsResource(property);
        if (ref != null && ref.getAsResource("location") != null
            && ref.getAsResource("location").getAsResource("geo") != null) {
          location = ref.getAsResource("location");
          break;
        }
      }
    }

    if (location == null || location.getAsResource("geo") == null) {
      return null;
    }

    ObjectNode node = new ObjectNode(JsonNodeFactory.instance);
    ObjectNode geometry = new ObjectNode(JsonNodeFactory.instance);
    ArrayNode coordinates = new ArrayNode(JsonNodeFactory.instance);
    ObjectNode properties = new ObjectNode(JsonNodeFactory.instance);
    coordinates.add(Double.valueOf(location.getAsResource("geo").getAsString("lon")));
    coordinates.add(Double.valueOf(location.getAsResource("geo").getAsString("lat")));
    node.put("type", "Feature");
    node.set("geometry", geometry);
    node.set("properties", properties);
    node.put("id", resource.getId());
    geometry.put("type", "Point");
    geometry.set("coordinates", coordinates);
    properties.put("@id", resource.getId());
    properties.put("@type", resource.getType());
    if (resource.getAsResource("name") != null) {
      properties.set("name", resource.getAsResource("name").toJson());
    } else {
      properties.put("name", resource.getId());
    }
    return node;

  }

}
