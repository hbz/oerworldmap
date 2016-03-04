package services;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fo
 */
public class QueryContext {

  private Map<String, FilterBuilder> filters = new HashMap<>();
  private Map<String, List<AggregationBuilder>> aggregations = new HashMap<>();
  private List<String> roles = new ArrayList<>();
  private String[] fetchSource = new String[]{};

  public QueryContext(String userId, List<String> roles) {

    FilterBuilder authenticated = FilterBuilders.notFilter(FilterBuilders.orFilter(
      FilterBuilders.termFilter("about.@type", "Concept"),
      FilterBuilders.termFilter("about.@type", "ConceptScheme")));
    filters.put("authenticated", authenticated);

    // TODO: Remove privacy filter when all persons are accounts?
    FilterBuilder admin = FilterBuilders.notFilter(FilterBuilders.andFilter(
      FilterBuilders.termFilter("about.@type", "Person"),
      FilterBuilders.notFilter(FilterBuilders.existsFilter("about.email"))));
    filters.put("admin", admin);

    /*
    if (userId != null) {
      FilterBuilder owner = FilterBuilders.notFilter(FilterBuilders.andFilter(
        FilterBuilders.termFilter("about.@type", "Person"),
        FilterBuilders.notFilter(FilterBuilders.termFilter("about." + JsonLdConstants.ID, userId))));
      filters.put("owner", owner);
    }
    */

    List<AggregationBuilder> guestAggregations = new ArrayList<>();
    guestAggregations.add(AggregationProvider.getTypeAggregation());
    guestAggregations.add(AggregationProvider.getByCountryAggregation());
    guestAggregations.add(AggregationProvider.getServiceLanguageAggregation());
    guestAggregations.add(AggregationProvider.getServiceByTopLevelFieldOfEducationAggregation());
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

  public List< AggregationBuilder> getAggregations() {
    List<AggregationBuilder> appliedAggregations = new ArrayList<>();
    for (Map.Entry<String, List<AggregationBuilder>> entry : aggregations.entrySet()) {
      if (roles.contains(entry.getKey())) {
        for (AggregationBuilder aggregation : entry.getValue()) {
          if (!appliedAggregations.contains(aggregation)) {
            appliedAggregations.add(aggregation);
          }
        }
      }
    }
    return appliedAggregations;
  }

}
