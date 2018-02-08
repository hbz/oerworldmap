package models;

import com.google.common.base.Joiner;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import play.Logger;

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

  private static URIBuilder getURIBuilder() throws URISyntaxException {
    return new URIBuilder("/resource/");
  }

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

  private void addParam(List<NameValuePair> params, String name, String value) {
    if (!StringUtils.isEmpty(value)) {
      params.add(new BasicNameValuePair(name, value));
    }
  }

  private URI getCurrentPage() throws URISyntaxException {
    List<NameValuePair> params = new ArrayList<>();

    addParam(params, "q", query);
    addParam(params, "from", Long.toString(from));
    addParam(params, "size", Long.toString(size));
    addParam(params, "sort", sort);
    params.addAll(getFilterParams());

    return getURIBuilder().setParameters(params).build();
  }

  private URI getNextPage() throws URISyntaxException {
    if (size == -1 || from + size >= totalItems) {
      return null;
    }

    List<NameValuePair> params = new ArrayList<>();
    addParam(params, "q", query);
    addParam(params, "from", Long.toString(from + size));
    addParam(params, "size", Long.toString(size));
    addParam(params, "sort", sort);
    params.addAll(getFilterParams());

    return params.isEmpty() ? null : getURIBuilder().addParameters(params).build();
  }


  private URI getPreviousPage() throws URISyntaxException {
    if (size == -1 || from - size < 0) {
      return null;
    }

    List<NameValuePair> params = new ArrayList<>();
    addParam(params, "q", query);
    addParam(params, "from", Long.toString(from - size));
    addParam(params, "size", Long.toString(size));
    addParam(params, "sort", sort);
    params.addAll(getFilterParams());

    return params.isEmpty() ? null : getURIBuilder().addParameters(params).build();
  }

  private URI getFirstPage() throws URISyntaxException {
    if (size == -1 || from <= 0) {
      return null;
    }

    List<NameValuePair> params = new ArrayList<>();
    addParam(params, "q", query);
    addParam(params, "from", Long.toString(0));
    addParam(params, "size", Long.toString(size));
    addParam(params, "sort", sort);
    params.addAll(getFilterParams());

    return params.isEmpty() ? null : getURIBuilder().addParameters(params).build();
  }

  private URI getLastPage() throws URISyntaxException {
    if (size == -1 || from + size >= totalItems) {
      return null;
    }

    List<NameValuePair> params = new ArrayList<>();
    addParam(params, "q", query);

    if (size > 0 && (totalItems / size) * size == totalItems) {
      addParam(params, "from", Long.toString((totalItems / size) * size - size));
    } else if (size > 0) {
      addParam(params, "from", Long.toString((totalItems / size) * size));
    } else {
      addParam(params, "from", Long.toString(0));
    }

    addParam(params, "size", Long.toString(size));
    addParam(params, "sort", sort);
    params.addAll(getFilterParams());

    return params.isEmpty() ? null : getURIBuilder().addParameters(params).build();
  }

  private String getFrom() {
    return Integer.toString(from);
  }

  private String getSize() {
    return Long.toString(size);
  }

  private List<URI> getPages() throws URISyntaxException {
    if (size <= 0) {
      return Collections.singletonList(getCurrentPage());
    }

    List<URI> pages = new ArrayList<>();

    List<NameValuePair> params = new ArrayList<>();
    addParam(params, "q", query);
    addParam(params, "size", Long.toString(size));
    addParam(params, "sort", sort);
    params.addAll(getFilterParams());

    for (int i = 0; i <= totalItems; i += size) {
      List<NameValuePair>pageParams = new ArrayList<>();
      pageParams.addAll(params);
      pageParams.add(new BasicNameValuePair("from", Integer.toString(i)));
      pages.add(getURIBuilder().addParameters(pageParams).build());
    }

    return pages;
  }

  private List<NameValuePair> getFilterParams() {
    List<NameValuePair> params = new ArrayList<>();

    if (filters != null) {
      for (Map.Entry<String, List<String>> entry : filters.entrySet()) {
        for (String filter : entry.getValue()) {
          if (!StringUtils.isEmpty(filter)) {
            params.add(new BasicNameValuePair("filter.".concat(entry.getKey()), filter));
          }
        }
      }
    }

    return params;
  }

  public Resource toResource() {
    Resource pagedCollection = new Resource("PagedCollection");

    try {
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
    } catch (URISyntaxException e) {
      Logger.error("Failed to build URI", e);
    }

    return pagedCollection;
  }

  public boolean containsType(String aType) {
    for (Resource item : items) {
      if (item.getAsResource("about") != null && aType.equals(item.getAsResource("about").getType())) {
        return true;
      }
    }
    return false;
  }

}
