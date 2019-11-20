package services.export;

import models.Record;
import models.Resource;
import models.ResourceList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class CsvExporter implements Exporter {

  private List<Pattern> exposedHeaders;

  public CsvExporter() {
    exposedHeaders = Arrays.asList(
      Pattern.compile("/@id"),
      Pattern.compile("/@type"),
      Pattern.compile("/name/en"),
      Pattern.compile("/description/en"),
      Pattern.compile("/provider/\\d+/name/en"),
      Pattern.compile("/url"),
      Pattern.compile("/additionalType/\\d+/name/en"),
      Pattern.compile("/primarySector/\\d+/name/en"),
      Pattern.compile("/startDate"),
      Pattern.compile("/endDate"),
      Pattern.compile("/startTime"),
      Pattern.compile("/endTime"),
      Pattern.compile("/agent/\\d+/name/en"),
      Pattern.compile("/location/\\d+/address/.*")
    );
  }

  public CsvExporter(List<Pattern> headers) {
    exposedHeaders = headers;
  }

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
      Map<String, String> pointerDict = resource.getAsResource(Record.RESOURCE_KEY).toPointerDict();
      pointerDicts.add(pointerDict);
      headers.addAll(pointerDict.keySet());
    }
    Iterator<String> it = headers.iterator();
    while(it.hasNext()) {
      String header = it.next();
      boolean discard = true;
      for (Pattern exposedHeader: exposedHeaders) {
        if (exposedHeader.matcher(header).matches()) {
          discard = false;
          break;
        }
      }
      if (discard) it.remove();
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

}
