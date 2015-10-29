package services.export;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeSet;

import helpers.JsonLdConstants;
import models.Resource;

public class CsvExporter {

  private static String mPathSeparator = ">";

  private TreeSet<String> mKeys = new TreeSet<>();
  private String[] mValues = new String[0];

  public String exportResourceAsCsvLine(Resource aResource) {
    if (mKeys.isEmpty()) {
      throw new IllegalStateException(
          "Trying to export Resource as CSV before having headers been set up: \n" + aResource);
    } //
    else {
      mValues = new String[mKeys.size()];
      Iterator<Entry<String, Object>> it = aResource.entrySet().iterator();
      while (it.hasNext()) {
        flattenResourceElement(it.next());
      }
      return fieldValuesToCsvString();
    }
  }

  private void flattenResourceElement(Entry<String, Object> aResourceEntry) {
    Object value = aResourceEntry.getValue();
    int index = getIndexInHeader(aResourceEntry.getKey());
    if (index == -1) {
      throw new IllegalStateException(
          "Trying to export Resource entry but key not found in header: \n" + aResourceEntry);
    } //
    else {
      if (value instanceof List<?>) {
        ArrayList<?> values = (ArrayList<?>) value;
        if (!values.isEmpty()) {
          String valueList = toExportString(values.get(0));
          for (int i = 1; i < values.size(); i++) {
            valueList.concat("; ").concat(toExportString(values.get(i)));
          }
          mValues[index] = valueList;
        }
      } //
      else {
        mValues[index] = toExportString(value);
      }
    }
  }

  private static String toExportString(Object value) {
    StringBuilder result = new StringBuilder();
    if (value instanceof Resource) {
      Resource resource = (Resource) value;
      if (resource.get(JsonLdConstants.ID) != null) {
        String id = resource.get(JsonLdConstants.ID).toString();
        // only export IDs of nested Resources
        result.append(id.toString());
      } //
      else {
        // Resource without ID (e. g. "location"): export all sub fields flatly
        // Presumes that all sub fields of a non-ID Resource are of String type,
        // will export nonsense otherwise.
        // The order of exported subfields is not predicted as sub Resources do
        // not necessarily contain the same fields.
        TreeSet<Object> values = new TreeSet<>(resource.values());
        for (int i = 0; i < values.size();) {
          result.append(values.pollFirst());
          if (i < values.size()) {
            result.append(", ");
          }
        }
      }
    } //
    else {
      result.append(value.toString());
    }
    return result.toString();
  }

  public void defineHeaderColumns(List<Resource> aResourceList) {
    mKeys.clear();
    for (Resource resource : aResourceList) {
      Iterator<Entry<String, Object>> it = resource.entrySet().iterator();
      while (it.hasNext()) {
        flattenKeys(it.next());
      }
    }
  }

  private void flattenKeys(Entry<String, Object> aResourceEntry) {
    mKeys.add(aResourceEntry.getKey());
  }

  private int getIndexInHeader(String aFieldName) {
    return mKeys.contains(aFieldName) ? mKeys.headSet(aFieldName).size() : -1;
  }

  private String fieldValuesToCsvString() {
    StringBuffer csv = new StringBuffer("");
    if (mValues.length > 0) {
      csv.append(mValues[0]);
    }
    for (int i = 1; i < mValues.length; i++) {
      csv.append(";").append(mValues[i]);
    }
    return csv.toString();
  }

  public String headerKeysToCsvString() {
    StringBuffer header = new StringBuffer("");
    if (mKeys.size() > 0) {
      header.append(mKeys.pollFirst());
    }
    while (!mKeys.isEmpty()) {
      header.append(";").append(mKeys.pollFirst());
    }
    return header.toString();
  }

}
