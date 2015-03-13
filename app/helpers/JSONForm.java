package helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import models.Resource;
import scala.util.parsing.json.JSONObject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class implements the application/json encoding algorithm described in
 * http://www.w3.org/TR/html-json-forms/#the-application-json-encoding-algorithm
 *
 * @author fo
 */
public class JSONForm {

  public static JsonNode parseFormData(Map<String,String[]> formData) {
    List<JsonNode> results = new ArrayList<>();
    for (Map.Entry<String,String[]> entry : formData.entrySet()) {
      JsonNode context = new ObjectNode(JsonNodeFactory.instance);
      String path = entry.getKey();
      String[] values = entry.getValue();
      List<JSONForm.Step> steps = parsePath(path);
      Collections.reverse(steps);
      //TODO: implement file inputs
      boolean isFile = false;
      for (JSONForm.Step step : steps) {

        if (step.last) {
          ArrayNode vals = new ArrayNode(JsonNodeFactory.instance);
          for (String value : values) {
            vals.add(value);
          }
          if (step.type == Step.Type.Array) {
            int index = Integer.parseInt(step.key);
            ArrayNode object = new ArrayNode(JsonNodeFactory.instance);
            for (int i = 0; i < index; i++) {
              object.addNull();
            }
            if ((!step.append) && (vals.size() == 1)) {
              object.insert(index, vals.get(0));
            } else {
              object.insert(index, vals);
            }
            context = object;
          } else {
            ObjectNode object = new ObjectNode(JsonNodeFactory.instance);
            if ((!step.append) && (vals.size() == 1)) {
              object.set(step.key, vals.get(0));
            } else {
              object.set(step.key, vals);
            }
            context = object;
          }
        } else {
          if (step.type == Step.Type.Array) {
            int index = Integer.parseInt(step.key);
            ArrayNode object = new ArrayNode(JsonNodeFactory.instance);
            for (int i = 0; i < index; i++) {
              object.addNull();
            }
            object.insert(index, context);
            context = object;
          } else {
            ObjectNode object = new ObjectNode(JsonNodeFactory.instance);
            object.put(step.key, context);
            context = object;
          }
        }
      }
      results.add(context);
    }
    return merge(results);
  }

  public static List<Resource> generateErrorReport(ProcessingReport report) {
    List<Resource> errorReport = new ArrayList<>();
    for (ProcessingMessage message : report) {
      ObjectNode jsonMessage = (ObjectNode)message.asJson();
      ObjectNode instanceInfo = (ObjectNode)jsonMessage.get("instance");
      String path = pointerToPath(instanceInfo.get("pointer").textValue());
      instanceInfo.put("path", path);
      errorReport.add(Resource.fromJson(jsonMessage));
    }
    return errorReport;
  }

  private static String pointerToPath(String pointer) {

    if (pointer.equals("")) {
      return pointer;
    }

    if (pointer.substring(0, 1).equals("/")) {
      pointer = pointer.substring(1);
    }

    String[] parts = pointer.split("/");

    String path = parts[0];
    for (int i = 1; i < parts.length; i++) {
      path = path.concat("[".concat(parts[i]).concat("]"));
    }

    return path;

  }

  private static ObjectNode merge(ObjectNode x, ObjectNode y) {

    ObjectNode result = new ObjectNode(JsonNodeFactory.instance);
    Set<String> keys = new HashSet<>();
    Iterator itx = x.fieldNames();
    while (itx.hasNext()) {
      String key = itx.next().toString();
      keys.add(key);
    }
    Iterator ity = y.fieldNames();
    while (ity.hasNext()) {
      String key = ity.next().toString();
      keys.add(key);
    }
    for (String key : keys) {
      JsonNode valx = x.get(key);
      JsonNode valy = y.get(key);
      boolean nullx = (valx == null);
      boolean nully = (valy == null);

      if (nullx && !nully) {
        result.put(key, valy);
      } else if (nully && !nullx) {
        result.put(key, valx);
      } else if (valx instanceof ArrayNode && valy instanceof ArrayNode) {
        result.put(key, merge((ArrayNode)valx, (ArrayNode)valy));
      } else if (valx instanceof ObjectNode && valy instanceof ObjectNode) {
        result.put(key, merge((ObjectNode)valx, (ObjectNode)valy));
      } else if (!nullx) {
        ArrayNode val = new ArrayNode(JsonNodeFactory.instance);
        val.add(valx);
        val.add(valy);
        result.put(key, val);
      }
    }
    return result;

  }

  private static ArrayNode merge(ArrayNode x, ArrayNode y) {

    ArrayNode result = new ArrayNode(JsonNodeFactory.instance);
    int size = Math.max(x.size(), y.size());
    for (int i = 0; i < size; i++) {
      JsonNode valx = x.get(i);
      JsonNode valy = y.get(i);
      boolean nullx = (valx == null || valx.isNull());
      boolean nully = (valy == null || valy.isNull());

      if (nullx && nully) {
        result.addNull();
      } else if (nullx) {
        result.insert(i, valy);
      } else if (nully) {
        result.insert(i, valx);
      } else if (valx instanceof ArrayNode && valy instanceof ArrayNode) {
        result.insert(i, merge((ArrayNode) valx, (ArrayNode) valy));
      } else if (valx instanceof ObjectNode && valy instanceof ObjectNode) {
        result.insert(i, merge((ObjectNode) valx, (ObjectNode) valy));
      }
    }
    return result;

  }

  public static JsonNode merge(List<JsonNode> nodes) {
    ObjectNode merged = new ObjectNode(JsonNodeFactory.instance);
    for (JsonNode node : nodes) {
      merged = merge(merged, (ObjectNode)node);
    }
    return merged;
  }

  public static class Step {

    public static enum Type {
      Object, Array
    }

    public Type type;
    public Type nextType;
    public String key;
    public boolean last;
    public boolean append;

    public String toString() {
      return "{Type: " + type + ", Key: " + key + ", Last: " + last + ", Append: " + append + ", Next type: " + nextType + "}";
    }

  }

  public static List<Step> parsePath(String path) {

    String original = path;
    List<Step> steps = new ArrayList<>();

    String firstKey;
    int delimiter = path.indexOf("[");
    if (delimiter != -1) {
      firstKey = path.substring(0, delimiter);
      path = path.substring(delimiter);
    } else {
      firstKey = path;
      path = "";
    }

    if (firstKey.length() == 0) {
      return failParsePath(original);
    }

    Step firstStep = new Step();
    firstStep.type = Step.Type.Object;
    firstStep.key = firstKey;
    steps.add(firstStep);

    if (path.equals("")) {
      steps.get(steps.size() - 1).last = true;
      return steps;
    }

    Pattern keyPattern = Pattern.compile("^\\[([^\\]]*)\\]");
    while (!path.equals("")) {
      Matcher keyMatcher = keyPattern.matcher(path);
      if (keyMatcher.find()) {
        String keyPart = keyMatcher.group(1);
        if (keyPart.equals("")) {
          steps.get(steps.size() - 1).append = true;
          path = path.substring(2);
          if (path.length() > 0) {
            return failParsePath(original);
          }
        } else {
          Step step = new Step();
          step.key = keyPart;
          try {
            Integer.parseInt(keyPart);
            step.type = Step.Type.Array;
          } catch (NumberFormatException e) {
            step.type = Step.Type.Object;
          }
          steps.add(step);
          path = path.substring(keyPart.length() + 2);
        }
      } else {
        return failParsePath(original);
      }
    }

    for (int i = 0; i < steps.size(); i++) {
      if (i == steps.size() - 1) {
        steps.get(i).last = true;
      } else {
        steps.get(i).nextType = steps.get(i + 1).type;
      }
    }

    return steps;

  }

  private static List<Step> failParsePath(String path) {
    List<Step> steps = new ArrayList<>();
    Step step = new Step();
    step.type = Step.Type.Object;
    step.last = true;
    step.key = path;
    steps.add(step);
    return steps;
  }

}
