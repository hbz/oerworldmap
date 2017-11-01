package services.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.Record;
import models.Resource;
import models.ResourceList;

import java.util.List;

/**
 * Created by fo on 27.03.17.
 */
public class GeoJsonExporter implements Exporter {

  @Override
  public String export(Resource aResource) {
    JsonNode node = toGeoJson(aResource, true);
    return node == null ? null : node.toString();
  }

  @Override
  public String export(ResourceList aResourceList) {
    return exportJson(aResourceList.getItems()).toString();
  }

  public JsonNode exportJson(Resource aResource) {
    return toGeoJson(aResource, false);
  }

  public JsonNode exportJson(ResourceList aResourceList) {
    return exportJson(aResourceList.getItems());
  }

  private JsonNode exportJson(List<Resource> aResources) {
    ObjectNode node = new ObjectNode(JsonNodeFactory.instance);
    ArrayNode features = new ArrayNode(JsonNodeFactory.instance);
    node.put("type", "FeatureCollection");
    for (Resource resource : aResources) {
      JsonNode feature = toGeoJson(resource, false);
      if (feature != null) {
        features.add(feature);
      }
    }
    node.set("features", features);
    return node;
  }

  private JsonNode toGeoJson(Resource aResource, boolean expand) {

    JsonNode resource = aResource.getAsResource(Record.RESOURCE_KEY).toJson();
    ArrayNode coordinates = getCoordinates(resource);

    if (coordinates.size() == 0) {
      return null;
    }

    ObjectNode node = new ObjectNode(JsonNodeFactory.instance);

    if (expand) {
      node.set("properties", aResource.toJson());
    } else {
      ObjectNode properties = new ObjectNode(JsonNodeFactory.instance);
      properties.set("@id", resource.get("@id"));
      properties.set("@type", resource.get("@type"));
      if (resource.has("name")) {
        properties.set("name", resource.get("name"));
      }
      node.set("properties", properties);
    }

    node.put("type", "Feature");
    node.set("id", resource.get("@id"));
    ObjectNode geometry = new ObjectNode(JsonNodeFactory.instance);
    geometry.put("type", coordinates.size() > 1 ? "MultiPoint" : "Point");
    geometry.set("coordinates", coordinates.size() > 1 ? coordinates: coordinates.get(0));
    node.set("geometry", geometry);

    return node;

  }

  private ArrayNode getCoordinates(JsonNode node) {

    ArrayNode coordinates = new ArrayNode(JsonNodeFactory.instance);
    String[] traverse = new String[] {"mentions", "member", "agent", "participant", "provider"};

    if (node.isArray()) {
      for (JsonNode entry : node) {
        coordinates.addAll(getCoordinates(entry));
      }
    } else if (node.isObject()) {
      if (node.has("location") && node.get("location").has("geo")) {
        ArrayNode result = new ArrayNode(JsonNodeFactory.instance);
        ObjectNode geo = (ObjectNode) node.get("location").get("geo");
        result.add(geo.get("lon").asDouble());
        result.add(geo.get("lat").asDouble());
        coordinates.add(result);
      } else {
        for (String property : traverse) {
          if (node.has(property)) {
            ArrayNode ref = getCoordinates(node.get(property));
            if (ref != null) {
              coordinates.addAll(ref);
            }
          }
        }
      }
    }

    return coordinates;

  }

}
