package models;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

/**
 * @author fo
 */
public class ResourceList extends ModelCommonList{

  public ResourceList(@Nonnull List<ModelCommon> aResourceList, long aTotalItems, String aQuery, int aFrom,
                      int aSize, String aSort, Map<String, List<String>> aFilters, Resource aAggregations) {
    super(aResourceList, aTotalItems, aQuery, aFrom, aSize, aSort, aFilters, aAggregations);
  }

  public ResourceList(Resource aPagedCollection) {
    super(aPagedCollection);
  }

  private String getCurrentPage() {
    Map<String, Object> params = buildParamsMap(from);
    return params.isEmpty() ? null : "/resource/?".concat(joiner.join(params));
  }

  private String getNextPage() {
    if (from + size >= totalItems) {
      return null;
    }
    Map<String, Object> params = buildParamsMap(from + size);
    return params.isEmpty() ? null : "/resource/?".concat(joiner.join(params));
  }

  private String getPreviousPage() {
    if (from - size < 0) {
      return null;
    }
    Map<String, Object> params = buildParamsMap(from - size);
    return params.isEmpty() ? null : "/resource/?".concat(joiner.join(params));
  }

  private String getFirstPage() {
    if (from <= 0) {
      return null;
    }
    Map<String, Object> params = buildParamsMap(Long.valueOf(0));
    return params.isEmpty() ? null : "/resource/?".concat(joiner.join(params));
  }

  private String getLastPage(){
    return super.getLastPage("resource");
  }

  private List<String> getPages(){
    return super.getPages("resource");
  }

  public Resource toResource() {
    Resource pagedCollection = new Resource("PagedCollection");
    pagedCollection.put("totalItems", totalItems);
    pagedCollection.put("size", Long.toString(size));
    pagedCollection.put("currentPage", getCurrentPage());
    pagedCollection.put("nextPage", getNextPage());
    pagedCollection.put("previousPage", getPreviousPage());
    pagedCollection.put("lastPage", getLastPage());
    pagedCollection.put("firstPage", getFirstPage());
    pagedCollection.put("from", Long.toString(from));
    pagedCollection.put("member", items);
    pagedCollection.put("filters", filters);
    pagedCollection.put("query", query);
    pagedCollection.put("aggregations", aggregations);
    pagedCollection.put("pages", getPages());
    return pagedCollection;
  }

}
