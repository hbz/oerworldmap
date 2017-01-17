package models;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author fo
 */
public class ResourceList {

  private List<Resource> items;

  private long totalItems;

  private long itemsPerPage;

  private String sortOrder;

  private String searchTerms;

  private int offset;

  private Map<String, List<String>> filters;

  private Resource aggregations;

  public ResourceList(@Nonnull List<Resource> aResourceList, long aTotalItems, String aSearchTerms, int aOffset,
                      int aSize, String aSortOrder, Map<String, List<String>> aFilters, Resource aAggregations) {
    items = aResourceList;
    totalItems = aTotalItems;
    searchTerms = aSearchTerms;
    offset = aOffset;
    itemsPerPage = aSize;
    sortOrder = aSortOrder;
    filters = aFilters;
    aggregations = aAggregations;
  }

  public ResourceList(Resource aPagedCollection) {
    items = aPagedCollection.getAsList("member");
    totalItems = Long.valueOf(aPagedCollection.getAsString("totalItems"));
    searchTerms = aPagedCollection.getAsString("searchTerms");
    offset = Integer.valueOf(aPagedCollection.getAsString("from"));
    if (offset > 0) {
      offset--;
      itemsPerPage = Integer.valueOf(aPagedCollection.getAsString("until")) - offset;
    }
    itemsPerPage = Integer.valueOf(aPagedCollection.getAsString("itemsPerPage"));
    aggregations = aPagedCollection.getAsResource("aggregations");
    filters = (Map<String, List<String>>) aPagedCollection.getAsMap("filters");
  }

  public List<Resource> getItems() {
    return this.items;
  }

  private String getCurrentPage() {
    ArrayList<String> params = new ArrayList<>();
    if (!StringUtils.isEmpty(searchTerms)) {
      params.add("q=".concat(searchTerms));
    }
    params.add("from=".concat(Long.toString(offset)));
    params.add("size=".concat(Long.toString(itemsPerPage)));
    if (!StringUtils.isEmpty(sortOrder)) {
      params.add("sort=".concat(sortOrder));
    }
    if (!(null == filters)) {
      for (Map.Entry<String, List<String>> entry : filters.entrySet()) {
        for (String filter : entry.getValue()) {
          params.add("filter.".concat(entry.getKey()).concat("=").concat(filter));
        }
      }
    }
    return params.isEmpty() ? null : "/resource/?".concat(StringUtils.join(params, "&"));
  }

  private String getNextPage() {
    if (offset + itemsPerPage >= totalItems) {
      return null;
    }

    ArrayList<String> params = new ArrayList<>();
    if (!StringUtils.isEmpty(searchTerms)) {
      params.add("q=".concat(searchTerms));
    }
    params.add("from=".concat(Long.toString(offset + itemsPerPage)));
    params.add("size=".concat(Long.toString(itemsPerPage)));
    if (!StringUtils.isEmpty(sortOrder)) {
      params.add("sort=".concat(sortOrder));
    }
    if (!(null == filters)) {
      for (Map.Entry<String, List<String>> entry : filters.entrySet()) {
        for (String filter : entry.getValue()) {
          params.add("filter.".concat(entry.getKey()).concat("=").concat(filter));
        }
      }
    }
    return params.isEmpty() ? null : "/resource/?".concat(StringUtils.join(params, "&"));
  }


  private String getPreviousPage() {
    if (offset - itemsPerPage < 0) {
      return null;
    }

    ArrayList<String> params = new ArrayList<>();
    if (!StringUtils.isEmpty(searchTerms)) {
      params.add("q=".concat(searchTerms));
    }
    params.add("from=".concat(Long.toString(offset - itemsPerPage)));
    params.add("size=".concat(Long.toString(itemsPerPage)));
    if (!StringUtils.isEmpty(sortOrder)) {
      params.add("sort=".concat(sortOrder));
    }
    if (!(null == filters)) {
      for (Map.Entry<String, List<String>> entry : filters.entrySet()) {
        for (String filter : entry.getValue()) {
          params.add("filter.".concat(entry.getKey()).concat("=").concat(filter));
        }
      }
    }
    return params.isEmpty() ? null : "/resource/?".concat(StringUtils.join(params, "&"));
  }

  private String getFirstPage() {
    if (offset <= 0) {
      return null;
    }

    ArrayList<String> params = new ArrayList<>();
    if (!StringUtils.isEmpty(searchTerms)) {
      params.add("q=".concat(searchTerms));
    }
    params.add("from=0");
    params.add("size=".concat(Long.toString(itemsPerPage)));
    if (!StringUtils.isEmpty(sortOrder)) {
      params.add("sort=".concat(sortOrder));
    }
    if (!(null == filters)) {
      for (Map.Entry<String, List<String>> entry : filters.entrySet()) {
        for (String filter : entry.getValue()) {
          params.add("filter.".concat(entry.getKey()).concat("=").concat(filter));
        }
      }
    }
    return params.isEmpty() ? null : "/resource/?".concat(StringUtils.join(params, "&"));
  }

  private String getLastPage() {
    if (offset + itemsPerPage >= totalItems) {
      return null;
    }

    ArrayList<String> params = new ArrayList<>();
    if (!StringUtils.isEmpty(searchTerms)) {
      params.add("q=".concat(searchTerms));
    }
    if ((totalItems / itemsPerPage) * itemsPerPage == totalItems) {
      params.add("from=".concat(Long.toString((totalItems / itemsPerPage) * itemsPerPage - itemsPerPage)));
    } else {
      params.add("from=".concat(Long.toString((totalItems / itemsPerPage) * itemsPerPage)));
    }
    params.add("size=".concat(Long.toString(itemsPerPage)));
    if (!StringUtils.isEmpty(sortOrder)) {
      params.add("sort=".concat(sortOrder));
    }
    if (!(null == filters)) {
      for (Map.Entry<String, List<String>> entry : filters.entrySet()) {
        for (String filter : entry.getValue()) {
          params.add("filter.".concat(entry.getKey()).concat("=").concat(filter));
        }
      }
    }
    return params.isEmpty() ? null : "/resource/?".concat(StringUtils.join(params, "&"));
  }

  private String getFrom() {
    return Integer.toString(this.offset + 1);
  }

  private String getUntil() {
    if(this.offset + this.itemsPerPage < this.totalItems) {
      return Long.toString(this.offset + this.itemsPerPage);
    } else {
      return Long.toString(this.totalItems);
    }

  }

  public Resource toResource() {
    Resource pagedCollection = new Resource("PagedCollection");
    pagedCollection.put("totalItems", totalItems);
    pagedCollection.put("itemsPerPage", itemsPerPage);
    pagedCollection.put("currentPage", getCurrentPage());
    pagedCollection.put("nextPage", getNextPage());
    pagedCollection.put("previousPage", getPreviousPage());
    pagedCollection.put("lastPage", getLastPage());
    pagedCollection.put("firstPage", getFirstPage());
    pagedCollection.put("from", getFrom());
    pagedCollection.put("until", getUntil());
    pagedCollection.put("member", items);
    pagedCollection.put("filters", filters);
    pagedCollection.put("searchTerms", searchTerms);
    pagedCollection.put("aggregations", aggregations);
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
