package services.export;

import helpers.JsonLdConstants;
import models.Resource;
import models.ResourceList;

import java.util.*;
import java.util.Map.Entry;

public class CsvDetailedExporter implements AbstractCsvExporter {

  private static String mPathSeparator = ">";

  private TreeSet<String> mKeys = new TreeSet<>();
  private String[] mValues = new String[0];
  private List<String> mDropFields = new ArrayList<>();

  @Override
  public String export(ResourceList aResourceList){
    StringBuffer result = new StringBuffer();
    defineHeaderColumns(aResourceList.getItems());
    setDropFields(Arrays.asList(JsonLdConstants.TYPE));
    result.append(headerKeysToCsvString().concat("\n"));
    for (Resource resource : aResourceList.getItems()) {
      result.append(export(resource).concat("\n"));
    }
    return result.toString();
  }

  @Override
  public String export(Resource aResource) {
    if (mKeys.isEmpty()) {
      throw new IllegalStateException(
          "Trying to export Resource as CSV before having headers been set up: \n" + aResource);
    } //
    else {
      mValues = new String[mKeys.size()];
      Iterator<Entry<String, Object>> it = aResource.entrySet().iterator();
      while (it.hasNext()) {
        flattenResourceElement(it.next(), "");
      }
      return fieldValuesToCsvString();
    }
  }

  private void flattenResourceElement(Entry<String, Object> aResourceEntry, String aKeyPath) {
    if (aResourceEntry.getValue() instanceof String) {
      int index = getIndexInHeader(aKeyPath + aResourceEntry.getKey());
      if (index == -1) {
        throw new IllegalStateException(
            "Trying to export Resource entry but key not found in header: \n" + aResourceEntry);
      } else {
        mValues[index] = (String) aResourceEntry.getValue();
      }
    } //
    else if (aResourceEntry.getValue() instanceof Resource) {
      Iterator<Entry<String, Object>> it = ((Resource) aResourceEntry.getValue()).entrySet()
          .iterator();
      while (it.hasNext()) {
        flattenResourceElement(it.next(), aKeyPath + aResourceEntry.getKey() + mPathSeparator);
      }
    } //
    else if (aResourceEntry.getValue() instanceof List<?>) {
      ArrayList<?> values = (ArrayList<?>) aResourceEntry.getValue();
      for (int i = 0; i < values.size(); i++) {
        Object value = values.get(i);
        if (value instanceof Resource) {
          Iterator<Entry<String, Object>> it = ((Resource) value).entrySet().iterator();
          while (it.hasNext()) {
            flattenResourceElement(it.next(),
                aKeyPath + aResourceEntry.getKey() + mPathSeparator + i + mPathSeparator);
          }
        } //
        else if (value instanceof String) {
          int index = getIndexInHeader(aKeyPath + aResourceEntry.getKey() + mPathSeparator + i);
          if (index == -1) {
            throw new IllegalStateException(
                "Trying to export List in Resource entry but key not found in header: \n" + values);
          } else {
            mValues[index] = (String) value;
          }
        }
      }
    }
  }

  public void defineHeaderColumns(List<Resource> aResourceList) {
    mKeys.clear();
    for (Resource resource : aResourceList) {
      Iterator<Entry<String, Object>> it = resource.entrySet().iterator();
      while (it.hasNext()) {
        flattenKeys(it.next(), "");
      }
    }
  }

  private void flattenKeys(Entry<String, Object> aResourceEntry, String aKeyPath) {
    if (aResourceEntry.getValue() instanceof String) {
      mKeys.add(aKeyPath + aResourceEntry.getKey());
    } //
    else if (aResourceEntry.getValue() instanceof Resource) {
      Iterator<Entry<String, Object>> it = ((Resource) aResourceEntry.getValue()).entrySet()
          .iterator();
      while (it.hasNext()) {
        flattenKeys(it.next(), aKeyPath + aResourceEntry.getKey() + mPathSeparator);
      }
    } //
    else if (aResourceEntry.getValue() instanceof List<?>) {
      ArrayList<?> values = (ArrayList<?>) aResourceEntry.getValue();
      for (int i = 0; i < values.size(); i++) {
        Object value = values.get(i);
        if (value instanceof Resource) {
          Iterator<Entry<String, Object>> it = ((Resource) value).entrySet().iterator();
          while (it.hasNext()) {
            flattenKeys(it.next(),
                aKeyPath + aResourceEntry.getKey() + mPathSeparator + i + mPathSeparator);
          }
        } else if (value instanceof String) {
          mKeys.add(aKeyPath + aResourceEntry.getKey() + mPathSeparator + i);
        }
      }
    }
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
    for (String key : mKeys) {
      header.append(key);
      if (mKeys.tailSet(key).size() > 0) {
        header.append(";");
      }
    }
    return header.toString();
  }

  @Override
  public void setDropFields(List<String> aDropFields) {
    mDropFields.clear();
    mDropFields.addAll(aDropFields);
  }

}
