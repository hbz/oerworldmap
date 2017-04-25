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

    JsonNode resource = aResource.getAsResource(Record.RESOURCE_KEY).toJson();
    ArrayNode coordinates = getCoordinates(resource);

    if (coordinates == null) {
      return null;
    }

    ObjectNode node = new ObjectNode(JsonNodeFactory.instance);
    ObjectNode geometry = new ObjectNode(JsonNodeFactory.instance);
    ObjectNode properties = new ObjectNode(JsonNodeFactory.instance);
    node.put("type", "Feature");
    node.set("geometry", geometry);
    node.set("properties", properties);
    node.set("id", resource.get("@id"));
    geometry.put("type", "Point");
    geometry.set("coordinates", coordinates);
    properties.set("@id", resource.get("@id"));
    properties.set("@type", resource.get("@type"));
    if (resource.has("name")) {
      properties.set("name", resource.get("name"));
    }
    return node;

  }

  private ArrayNode getCoordinates(JsonNode node) {

    String[] traverse = new String[] {"mentions", "member", "agent", "participant", "provider"};

    if (node.isArray()) {
      for (JsonNode entry : node) {
        ArrayNode coordinates = getCoordinates(entry);
        if (coordinates != null) {
          return coordinates;
        }
      }
    } else if (node.isObject()) {
      if (node.has("location") && node.get("location").has("geo")) {
        ArrayNode result = new ArrayNode(JsonNodeFactory.instance);
        ObjectNode geo = (ObjectNode) node.get("location").get("geo");
        result.add(geo.get("lon").asDouble());
        result.add(geo.get("lat").asDouble());
        return result;
      } else {
        for (String property : traverse) {
          if (node.has(property)) {
            ArrayNode ref = getCoordinates(node.get(property));
            if (ref != null) {
              return ref;
            }
          }
        }
      }
    }

    return null;

  }

}
