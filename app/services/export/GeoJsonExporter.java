package services.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

  static final ObjectMapper mObjectMapper = new ObjectMapper();

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

  private JsonNode exportJson(List<Resource> aResources) {
    ObjectNode node = new ObjectNode(JsonNodeFactory.instance);
    ArrayNode features = new ArrayNode(JsonNodeFactory.instance);
    node.put("type", "FeatureCollection");
    for (Resource resource : aResources) {
      JsonNode feature = toGeoJson(resource, false);
      // Skip features without geometry
      if (feature != null && feature.has("geometry")) {
        features.add(feature);
      }
    }
    node.set("features", features);
    return node;
  }

  private JsonNode toGeoJson(Resource aResource, boolean expand) {

    JsonNode resource = aResource.getAsResource(Record.RESOURCE_KEY).toJson();

    ArrayNode locations;
    if (resource.has("@type") && resource.get("@type").asText().equals("Policy")) {
      JsonNode publisher = resource.get("publisher");
      locations = publisher == null ? mObjectMapper.createArrayNode() : getLocations(publisher);
    } else {
      locations = getLocations(resource);
    }

    if (locations.size() == 0) {
      return null;
    }

    ObjectNode node = new ObjectNode(JsonNodeFactory.instance);
    ObjectNode properties;

    if (expand) {
      properties = (ObjectNode) aResource.toJson();
    } else {
      properties = new ObjectNode(JsonNodeFactory.instance);
      properties.set("@id", resource.get("@id"));
      properties.set("@type", resource.get("@type"));
      if (resource.has("name")) {
        properties.set("name", resource.get("name"));
      }
      if (resource.has("image")) {
        properties.set("image", resource.get("image"));
      }
      if (resource.has("additionalType")) {
        properties.set("additionalType", resource.get("additionalType"));
      }
      if (resource.has("alternateName")) {
        properties.set("alternateName", resource.get("alternateName"));
      }
    }

    properties.set("location", locations.size() > 1 ? locations : locations.get(0));
    node.set("properties", properties);
    node.put("type", "Feature");
    node.set("id", resource.get("@id"));

    ArrayNode coordinates = getCoordinates(locations);
    if (coordinates.size() > 0) {
      ObjectNode geometry = new ObjectNode(JsonNodeFactory.instance);
      geometry.put("type", coordinates.size() > 1 ? "MultiPoint" : "Point");
      geometry.set("coordinates", coordinates.size() > 1 ? coordinates : coordinates.get(0));
      node.set("geometry", geometry);
    }

    return node;
  }

  private ArrayNode getCoordinates(ArrayNode locations) {

    ArrayNode coordinates = new ArrayNode(JsonNodeFactory.instance);

    for (JsonNode location : locations) {
      if (location.has("geo")) {
        ArrayNode result = new ArrayNode(JsonNodeFactory.instance);
        ObjectNode geo = (ObjectNode) location.get("geo");
        result.add(geo.get("lon").asDouble());
        result.add(geo.get("lat").asDouble());
        coordinates.add(result);
      }
    }

    return coordinates;
  }

  private ArrayNode getLocations(JsonNode node) {

    ArrayNode locations = new ArrayNode(JsonNodeFactory.instance);

    if (node.isArray()) {
      for (JsonNode entry : node) {
        locations.addAll(getLocations(entry));
      }
    } else if (node.isObject() && node.has("location")) {
      locations.addAll((ArrayNode) node.get("location"));
    }

    return locations;
  }
}
