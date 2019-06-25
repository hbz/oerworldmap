package services.export;

import com.fasterxml.jackson.databind.JsonNode;
import models.Record;
import models.Resource;
import models.ResourceList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class CsvExporter implements Exporter {
  @Override
  public String export(Resource aResource) {
    return export(Collections.singletonList(aResource));
  }

  @Override
  public String export(ResourceList aResourceList) {
    return export(aResourceList.getItems());
  }

  private String export(List<Resource> resources) {
    List<Map<String, String>> pointerDicts = new ArrayList<>();
    Set<String> headers = new TreeSet<>();
    for (Resource resource: resources) {
      Map<String, String> pointerDict = jsonNodeToPointerDict(
        resource.getAsResource(Record.RESOURCE_KEY).toJson(), "");
      pointerDicts.add(pointerDict);
      headers.addAll(pointerDict.keySet());
    }
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(String.join(",", headers)).append("\n");
    for (Map<String, String> pointerDict : pointerDicts) {
      List<String> values = new ArrayList<>();
      for (String header : headers) {
        String value = pointerDict.getOrDefault(header, "");
        values.add("\"".concat(value.replace("\"", "\"\"")).concat("\""));
      }
      stringBuilder.append(String.join(",", values)).append("\n");
    }
    return stringBuilder.toString();
  }

  private Map<String, String> jsonNodeToPointerDict(JsonNode node, String path) {
    Map<String, String> pointerDict = new HashMap<>();
    if (node.isArray()) {
      for (int i = 0; i < node.size(); i++) {
        pointerDict.putAll(jsonNodeToPointerDict(node.get(i), path.concat("/").concat(Integer.toString(i))));
      }
    } else if (node.isObject()) {
      Iterator<Map.Entry<String, JsonNode>> it = node.fields();
      while (it.hasNext()) {
        Map.Entry<String, JsonNode> entry = it.next();
        pointerDict.putAll(jsonNodeToPointerDict(entry.getValue(), path.concat("/").concat(entry.getKey())));
      }
    } else if (node.isValueNode()) {
      pointerDict.put(path, node.asText());
    }
    return pointerDict;
  }

}
