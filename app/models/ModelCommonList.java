package models;

import com.google.common.base.Joiner;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fo, pvb
 */


public abstract class ModelCommonList {

  protected Joiner.MapJoiner joiner = Joiner.on("&").withKeyValueSeparator("=");
  protected List<ModelCommon> items;
  protected long totalItems;
  protected long from;
  protected long size;
  protected String sort;
  protected String query;
  protected Map<String, List<String>> filters;
  protected ModelCommon aggregations;

  public ModelCommonList(@Nonnull List<ModelCommon> aResourceList, long aTotalItems, String aQuery, int aFrom,
                      int aSize, String aSort, Map<String, List<String>> aFilters, Resource aAggregations) {
    items = aResourceList;
    totalItems = aTotalItems;
    query = aQuery;
    from = aFrom;
    size = aSize;
    sort = aSort;
    filters = aFilters;
    aggregations = aAggregations;
  }

  public ModelCommonList(Resource aPagedCollection) {
    items = aPagedCollection.getAsList("member");
    totalItems = Long.valueOf(aPagedCollection.getAsString("totalItems"));
    query = aPagedCollection.getAsString("query");
    from = Integer.valueOf(aPagedCollection.getAsString("from"));
    if (from > 0) {
      from--;
      size = Integer.valueOf(aPagedCollection.getAsString("until")) - from;
    }
    size = Integer.valueOf(aPagedCollection.getAsString("size"));
    aggregations = aPagedCollection.getAsItem("aggregations");
    filters = (Map<String, List<String>>) aPagedCollection.getAsMap("filters");
  }

  public List<ModelCommon> getItems() {
    return items;
  }

  protected Map<String, String> buildParam(String name, String value) {
    Map<String, String> param = new HashMap<>();
    if (!StringUtils.isEmpty(value)) {
      param.put(name, value);
    }
    return param;
  }

  protected Map<String, Object> buildParamsMap(final Long aOffset) {
    Map<String, Object> params = new HashMap<>();
    params.putAll(buildParam("q", query));
    params.putAll(buildParam("from", Long.toString(aOffset)));
    params.putAll(buildParam("size", Long.toString(size)));
    params.putAll(buildParam("sort", sort));
    params.putAll(getFilterParams());
    return params;
  }

  protected Map<String, String> getFilterParams() {
    Map<String, String> params = new HashMap<>();
    if (filters != null) {
      for (Map.Entry<String, List<String>> entry : filters.entrySet()) {
        for (String filter : entry.getValue()) {
          params.put("filter.".concat(entry.getKey()), filter);
        }
      }
    }
    return params;
  }

  public boolean containsType(String aType) {
    for (ModelCommon item : items) {
      if (item.getAsItem("about").getType().equals(aType)) {
        return true;
      }
    }
    return false;
  }

  protected String getLastPage(final String aType) {
    if (from + size >= totalItems) {
      return null;
    }
    Map<String, Object> params = new HashMap<>();
    params.putAll(buildParam("q", query));
    if (size > 0 && (totalItems / size) * size == totalItems) {
      params.putAll(buildParam("from", Long.toString((totalItems / size) * size - size)));
    } else if (size > 0) {
      params.putAll(buildParam("from", Long.toString((totalItems / size) * size)));
    } else {
      params.putAll(buildParam("from", Long.toString(0)));
    }
    params.putAll(buildParam("size", Long.toString(size)));
    params.putAll(buildParam("sort", sort));
    params.putAll(getFilterParams());
    return params.isEmpty() ? null : "/" + aType + "/?".concat(joiner.join(params));
  }

  protected List<String> getPages(final String aType) {
    List<String> pages = new ArrayList<>();
    if (size == 0) {
      return pages;
    }
    Map<String, Object> params = new HashMap<>();
    params.putAll(buildParam("q", query));
    params.putAll(buildParam("size", Long.toString(size)));
    params.putAll(buildParam("sort", sort));
    params.putAll(getFilterParams());

    for (int i = 0; i <= totalItems; i += size) {
      Map<String, Object> pageParams = new HashMap<>();
      pageParams.putAll(params);
      pageParams.put("from", Integer.toString(i));
      pages.add("/" + aType + "/?".concat(joiner.join(pageParams)));
    }
    return pages;
  }

}
