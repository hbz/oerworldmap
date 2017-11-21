package services.export;

import helpers.JsonLdConstants;
import models.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeSet;

public class CsvWithNestedIdsExporter implements AbstractCsvExporter {

  private TreeSet<String> mKeys = new TreeSet<>();
  private String[] mValues = new String[0];
  private List<String> mDropFields = new ArrayList<>();

  @Override
  public String export(ModelCommonList aResourceList){
    StringBuffer result = new StringBuffer();
    List<ModelCommon> resources = new ArrayList<>();
    for (ModelCommon record : aResourceList.getItems()) {
      resources.add(record.getAsItem(Record.CONTENT_KEY));
    }
    defineHeaderColumns(resources);
    setDropFields(Arrays.asList(JsonLdConstants.TYPE));
    result.append(headerKeysToCsvString().concat("\n"));
    for (ModelCommon resource : resources) {
      result.append(buildRow(resource).concat("\n"));
    }
    return result.toString();
  }

  @Override
  public String export(ModelCommon aResource) {
    StringBuffer result = new StringBuffer();
    ModelCommon resource = aResource.getAsItem(Record.CONTENT_KEY);
    defineHeaderColumns(Arrays.asList(resource));
    setDropFields(Arrays.asList(JsonLdConstants.TYPE));
    result.append(headerKeysToCsvString().concat("\n"));
    result.append(buildRow(resource).concat("\n"));
    return result.toString();
  }

  private String buildRow(ModelCommon aResource) {
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
      return fieldValuesToCsvString(mValues);
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
          StringBuffer valueList = new StringBuffer(toExportString(values.get(0)));
          for (int i = 1; i < values.size(); i++) {
            valueList.append(", ").append(toExportString(values.get(i)));
          }
          mValues[index] = valueList.toString();
        }
      } //
      else {
        mValues[index] = toExportString(value);
      }
    }
  }

  private String toExportString(Object value) {
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
        // The order of exported subfields is not predicted as sub Resources do
        // not necessarily contain the same fields.
        result.append(((Resource) value).getValuesAsFlatString(",", mDropFields));
      }
    } //
    else {
      result.append(value.toString());
    }
    return result.toString();
  }

  @Override
  public void defineHeaderColumns(List<ModelCommon> aResourceList) {
    mKeys.clear();
    for (ModelCommon resource : aResourceList) {
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

  @Override
  public String headerKeysToCsvString() {
    TreeSet<String> keys = new TreeSet<>();
    keys.addAll(mKeys);
    StringBuffer header = new StringBuffer("");
    if (keys.size() > 0) {
      header.append(keys.pollFirst());
    }
    while (!keys.isEmpty()) {
      header.append(";").append(keys.pollFirst());
    }
    return header.toString();
  }

  @Override
  public void setDropFields(List<String> aDropFields) {
    mDropFields.clear();
    mDropFields.addAll(aDropFields);
  }

}
