package models;

import java.util.List;

/**
 * @author fo
 */
public class ResourceList {

  private List<Resource> items;

  private long totalItems;

  private long itemsPerPage;

  private String nextPage;

  private String previousPage;

  private String firstPage;

  private String lastPage;

  public List<Resource> getItems() {
    return this.items;
  }

  public void setItems(List<Resource> items) {
    this.items = items;
  }

  public long getTotalItems() {
    return this.totalItems;
  }

  public void setTotalItems(long totalItems) {
    this.totalItems = totalItems;
  }

  public long getItemsPerPage() {
    return this.itemsPerPage;
  }

  public void setItemsPerPage(long itemsPerPage) {
    this.itemsPerPage = itemsPerPage;
  }

  public String getNextPage() {
    return this.nextPage;
  }

  public void setNextPage(String nextPage) {
    this.nextPage = nextPage;
  }

  public String getPreviousPage() {
    return this.previousPage;
  }

  public String getFirstPage() {
    return this.firstPage;
  }

  public void setFirstPage(String firstPage) {
    this.firstPage = firstPage;
  }

  public String getLastPage() {
    return this.lastPage;
  }

  public void setLastPage(String lastPage) {
    this.lastPage = lastPage;
  }

  public void setPreviousPage(String previousPage) {
    this.previousPage = previousPage;
  }

  public Resource toResource() {
    Resource pagedCollection = new Resource("PagedCollection");
    pagedCollection.put("totalItems", totalItems);
    pagedCollection.put("itemsPerPage", itemsPerPage);
    pagedCollection.put("nextPage", nextPage);
    pagedCollection.put("previousPage", previousPage);
    pagedCollection.put("lastPage", lastPage);
    pagedCollection.put("firstPage", firstPage);
    pagedCollection.put("member", items);
    return pagedCollection;
  }

}
