package models;

import com.google.common.base.Joiner;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fo
 */
public class ResourceList {

  private Joiner.MapJoiner joiner = Joiner.on("&").withKeyValueSeparator("=");

  private List<Resource> items;

  private long totalItems;

  private int from;

  private long size;

  private String sort;

  private String query;

  private Map<String, List<String>> filters;

  private Resource aggregations;

  public ResourceList(@Nonnull List<Resource> aResourceList, long aTotalItems, String aQuery, int aFrom,
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

  public ResourceList(Resource aPagedCollection) {

    items = aPagedCollection.getAsList("member");
    totalItems = Long.valueOf(aPagedCollection.getAsString("totalItems"));
    query = aPagedCollection.getAsString("query");
    from = Integer.valueOf(aPagedCollection.getAsString("from"));
    if (from > 0) {
      from--;
      size = Integer.valueOf(aPagedCollection.getAsString("until")) - from;
    }
    size = Integer.valueOf(aPagedCollection.getAsString("totalItems"));
    aggregations = aPagedCollection.getAsResource("aggregations");
    filters = (Map<String, List<String>>) aPagedCollection.getAsMap("filters");

  }

  public List<Resource> getItems() {
    return items;
  }

  private Map<String, String> buildParam(String name, String value) {

    Map<String, String> param = new HashMap<>();
    if (!StringUtils.isEmpty(value)) {
      param.put(name, value);
    }

    return param;

  }

  private String getCurrentPage() {

    Map<String, Object> params = new HashMap<>();
    params.putAll(buildParam("q", query));
    params.putAll(buildParam("from", Long.toString(from)));
    params.putAll(buildParam("size", Long.toString(size)));
    params.putAll(buildParam("sort", sort));
    params.putAll(getFilterParams());

    return params.isEmpty() ? null : "/resource/?".concat(joiner.join(params));

  }

  private String getNextPage() {

    if (from + size >= totalItems) {
      return null;
    }

    Map<String, Object> params = new HashMap<>();
    params.putAll(buildParam("q", query));
    params.putAll(buildParam("from", Long.toString(from + size)));
    params.putAll(buildParam("size", Long.toString(size)));
    params.putAll(buildParam("sort", sort));
    params.putAll(getFilterParams());

    return params.isEmpty() ? null : "/resource/?".concat(joiner.join(params));

  }


  private String getPreviousPage() {

    if (from - size < 0) {
      return null;
    }

    Map<String, Object> params = new HashMap<>();
    params.putAll(buildParam("q", query));
    params.putAll(buildParam("from", Long.toString(from - size)));
    params.putAll(buildParam("size", Long.toString(size)));
    params.putAll(buildParam("sort", sort));
    params.putAll(getFilterParams());

    return params.isEmpty() ? null : "/resource/?".concat(joiner.join(params));

  }

  private String getFirstPage() {

    if (from <= 0) {
      return null;
    }

    Map<String, Object> params = new HashMap<>();
    params.putAll(buildParam("q", query));
    params.putAll(buildParam("from", Long.toString(0)));
    params.putAll(buildParam("size", Long.toString(size)));
    params.putAll(buildParam("sort", sort));
    params.putAll(getFilterParams());

    return params.isEmpty() ? null : "/resource/?".concat(joiner.join(params));

  }

  private String getLastPage() {

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

    return params.isEmpty() ? null : "/resource/?".concat(joiner.join(params));

  }

  private String getFrom() {
    return Integer.toString(from);
  }

  private String getSize() {
    return Long.toString(size);
  }

  private List<String> getPages() {

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
      pages.add("/resource/?".concat(joiner.join(pageParams)));
    }

    return pages;

  }

  private Map<String, String> getFilterParams() {

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

  public Resource toResource() {

    Resource pagedCollection = new Resource("PagedCollection");
    pagedCollection.put("totalItems", totalItems);
    pagedCollection.put("size", getSize());
    pagedCollection.put("currentPage", getCurrentPage());
    pagedCollection.put("nextPage", getNextPage());
    pagedCollection.put("previousPage", getPreviousPage());
    pagedCollection.put("lastPage", getLastPage());
    pagedCollection.put("firstPage", getFirstPage());
    pagedCollection.put("from", getFrom());
    pagedCollection.put("member", items);
    pagedCollection.put("filters", filters);
    pagedCollection.put("query", query);
    pagedCollection.put("aggregations", aggregations);
    pagedCollection.put("pages", getPages());

    return pagedCollection;

  }

  public boolean containsType(String aType) {
    for (Resource item : items) {
      if (item.getAsResource("about").getType().equals(aType)) {
        return true;
      }
    }
    return false;
  }

}
