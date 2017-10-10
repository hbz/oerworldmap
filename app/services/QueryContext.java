package services;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fo
 */
public class QueryContext {

  private Map<String, QueryBuilder> filters = new HashMap<>();
  private Map<String, List<AggregationBuilder<?>>> aggregations = new HashMap<>();
  private List<String> roles = new ArrayList<>();
  private String[] fetchSource = new String[] {};
  private String[] mElasticsearchFieldBoosts = new String[] {};
  private GeoPoint mZoomTopLeft = null;
  private GeoPoint mZoomBottomRight = null;
  private List<GeoPoint> mPolygonFilter = new ArrayList<>();

  public QueryContext(List<String> roles) {

    QueryBuilder concepts = QueryBuilders.boolQuery()
      .mustNot(QueryBuilders.termQuery("about.@type", "Concept"))
      .mustNot(QueryBuilders.termQuery("about.@type", "ConceptScheme"));

    filters.put("concepts", concepts);

    //QueryBuilder emptyNames = QueryBuilders.existsQuery("about.name");
    //filters.put("emptyNames", emptyNames);

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

  public List<QueryBuilder> getFilters() {
    List<QueryBuilder> appliedFilters = new ArrayList<>();
    for (Map.Entry<String, QueryBuilder> entry : filters.entrySet()) {
      if (!roles.contains(entry.getKey())) {
        appliedFilters.add(entry.getValue());
      }
    }
    return appliedFilters;
  }

  public List<AggregationBuilder<?>> getAggregations() {

    List<AggregationBuilder<?>> guestAggregations = new ArrayList<>();
    guestAggregations.add(AggregationProvider.getTypeAggregation(0));
    guestAggregations.add(
      filters.containsKey("iso3166")
        ? AggregationProvider.getRegionAggregation(0)
        : AggregationProvider.getByCountryAggregation(0)
    );
    guestAggregations.add(AggregationProvider.getServiceLanguageAggregation(0));
    guestAggregations.add(AggregationProvider.getServiceByTopLevelFieldOfEducationAggregation());
    guestAggregations.add(AggregationProvider.getServiceByGradeLevelAggregation(0));
    guestAggregations.add(AggregationProvider.getKeywordsAggregation(0));
    guestAggregations.add(AggregationProvider.getLicenseAggregation(0));
    guestAggregations.add(AggregationProvider.getEventCalendarAggregation());
    guestAggregations.add(AggregationProvider.getPrimarySectorsAggregation(0));
    guestAggregations.add(AggregationProvider.getSecondarySectorsAggregation(0));
    guestAggregations.add(AggregationProvider.getLocationAggregation(0));

    aggregations.put("guest", guestAggregations);
    aggregations.put("authenticated", guestAggregations);

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

  public void setIso3166Scope(String aISOCode) {
    QueryBuilder iso3166 = QueryBuilders.boolQuery()
      .must(QueryBuilders.termQuery("about.location.address.addressCountry", aISOCode));
    filters.put("iso3166", iso3166);
  }

}
