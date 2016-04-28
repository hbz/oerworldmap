package services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import play.Logger;

/**
 * @author fo
 */
public class QueryContext {

  private Map<String, FilterBuilder> filters = new HashMap<>();
  private Map<String, List<AggregationBuilder<?>>> aggregations = new HashMap<>();
  private List<String> roles = new ArrayList<>();
  private String[] fetchSource = new String[] {};
  private String[] mElasticsearchFieldBoosts = new String[] {};
  private GeoPoint mZoomTopLeft = null;
  private GeoPoint mZoomBottomRight = null;
  private List<GeoPoint> mPolygonFilter = new ArrayList<>();

  public QueryContext(String userId, List<String> roles) {

    FilterBuilder authenticated = FilterBuilders
        .notFilter(FilterBuilders.orFilter(FilterBuilders.termFilter("about.@type", "Concept"),
            FilterBuilders.termFilter("about.@type", "ConceptScheme")));
    filters.put("authenticated", authenticated);

    // TODO: Remove privacy filter when all persons are accounts?
    FilterBuilder admin = FilterBuilders
        .notFilter(FilterBuilders.andFilter(FilterBuilders.termFilter("about.@type", "Person"),
            FilterBuilders.notFilter(FilterBuilders.existsFilter("about.email"))));
    filters.put("admin", admin);

    /*
     * if (userId != null) { FilterBuilder owner =
     * FilterBuilders.notFilter(FilterBuilders.andFilter(
     * FilterBuilders.termFilter("about.@type", "Person"),
     * FilterBuilders.notFilter(FilterBuilders.termFilter("about." +
     * JsonLdConstants.ID, userId)))); filters.put("owner", owner); }
     */

    List<AggregationBuilder<?>> guestAggregations = new ArrayList<>();
    guestAggregations.add(AggregationProvider.getTypeAggregation());
    guestAggregations.add(AggregationProvider.getByCountryAggregation());
    guestAggregations.add(AggregationProvider.getServiceLanguageAggregation());
    guestAggregations.add(AggregationProvider.getServiceByFieldOfEducationAggregation());
    guestAggregations.add(AggregationProvider.getServiceByGradeLevelAggregation());
    guestAggregations.add(AggregationProvider.getTagAggregation());

    aggregations.put("guest", guestAggregations);
    aggregations.put("authenticated", guestAggregations);

    if (roles != null) {
      this.roles = roles;
    } else {
      this.roles.add("guest");
    }

  }

  public String[] getElasticsearchFieldBoosts() {
    return mElasticsearchFieldBoosts;
  }

  public void setElasticsearchFieldBoosts(String[] aElasticsearchFieldBoosts) {
    mElasticsearchFieldBoosts = aElasticsearchFieldBoosts;
  }

  public boolean hasFieldBoosts() {
    return mElasticsearchFieldBoosts.length > 0 && !StringUtils.isEmpty(mElasticsearchFieldBoosts[0]);
  }

  public String[] getFetchSource() {
    return this.fetchSource;
  }

  public void setFetchSource(String[] fetchSource) {
    this.fetchSource = fetchSource;
  }

  public List<FilterBuilder> getFilters() {
    List<FilterBuilder> appliedFilters = new ArrayList<>();
    for (Map.Entry<String, FilterBuilder> entry : filters.entrySet()) {
      if (!roles.contains(entry.getKey())) {
        appliedFilters.add(entry.getValue());
      }
    }
    return appliedFilters;
  }

  public List<AggregationBuilder<?>> getAggregations() {
    List<AggregationBuilder<?>> appliedAggregations = new ArrayList<>();
    for (Map.Entry<String, List<AggregationBuilder<?>>> entry : aggregations.entrySet()) {
      if (roles.contains(entry.getKey())) {
        for (AggregationBuilder<?> aggregation : entry.getValue()) {
          if (!appliedAggregations.contains(aggregation)) {
            appliedAggregations.add(aggregation);
          }
        }
      }
    }
    return appliedAggregations;
  }

  public GeoPoint getZoomBottomRight() {
    return mZoomBottomRight;
  }

  public void setZoomBottomRight(GeoPoint aZoomBottomRight) {
    mZoomBottomRight = aZoomBottomRight;
  }

  public GeoPoint getZoomTopLeft() {
    return mZoomTopLeft;
  }

  public void setZoomTopLeft(GeoPoint aZoomTopLeft) {
    mZoomTopLeft = aZoomTopLeft;
  }

  public List<GeoPoint> getPolygonFilter() {
	return mPolygonFilter;
  }

  /**
   * Set a Geo Polygon Filter for search.
   * The argument List<GeoPoint> may be empty in case of a filter reset but not null.
   * @param aPolygonFilter The Polygon Filter to be set.
   * @throws IllegalArgumentException if argument is null it consists of 1 or 2 GeoPoints.
   */
  public void setPolygonFilter(List<GeoPoint> aPolygonFilter) throws IllegalArgumentException {
    if (aPolygonFilter == null){
      throw new IllegalArgumentException("Argument null given as Polygon Filter.");
    }
    if (!aPolygonFilter.isEmpty() && aPolygonFilter.size() < 3){
      throw new IllegalArgumentException("Polygon Filter consisting of " + aPolygonFilter.size() + " GeoPoints only.");
    }
    mPolygonFilter = aPolygonFilter;
  }

  public void setBoundingBox(String aBoundingBox) throws NumberFormatException {
    String[] coordinates = aBoundingBox.split(",");
    if (coordinates.length == 4) {
      mZoomTopLeft = new GeoPoint(Double.parseDouble(coordinates[0]), Double.parseDouble(coordinates[1]));
      mZoomBottomRight = new GeoPoint(Double.parseDouble(coordinates[2]), Double.parseDouble(coordinates[3]));
    }
    throw new NumberFormatException();
  }

}
