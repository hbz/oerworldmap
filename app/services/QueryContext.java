package services;

import controllers.Global;
import controllers.Secured;
import helpers.JsonLdConstants;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import play.mvc.Http.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fo
 */
public class QueryContext {

  private Map<String, FilterBuilder> filters = new HashMap<>();
  private Map<String, AggregationBuilder> aggregations = new HashMap<>();
  private List<String> roles = new ArrayList<>();

  public QueryContext(String userId, List<String> roles) {

    FilterBuilder authenticated = FilterBuilders.notFilter(FilterBuilders.orFilter(
      FilterBuilders.termFilter("about.@type", "Concept"),
      FilterBuilders.termFilter("about.@type", "ConceptScheme")));
    filters.put("authenticated", authenticated);

    // TODO: Remove privacy filter when all persons are accounts?
    FilterBuilder admin = FilterBuilders.notFilter(FilterBuilders.andFilter(FilterBuilders
      .termFilter("about.@type", "Person"), FilterBuilders.notFilter(FilterBuilders.existsFilter("about.email"))));
    filters.put("admin", admin);

    if (userId != null) {
      FilterBuilder owner = FilterBuilders.termFilter("about." + JsonLdConstants.ID, userId);
      filters.put("owner", owner);
    }

    /*aggregations.put("guest", AggregationBuilders.global("agg")
        .subAggregation(AggregationProvider.getTypeAggregation())
        .subAggregation(AggregationProvider.getByCountryAggregation())
        .subAggregation(AggregationProvider.getServiceLanguageAggregation())
        .subAggregation(AggregationProvider.getFieldOfEducationAggregation());
    );*/
    this.roles = roles;
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
    /*for (Map.Entry<String, AggregationBuilder> entry : aggregations.entrySet()) {
      if (roles.contains(entry.getKey())) {
        appliedAggregations.add(entry.getValue());
      }
    }*/
    appliedAggregations.add(AggregationProvider.getTypeAggregation());
    appliedAggregations.add(AggregationProvider.getByCountryAggregation());
    appliedAggregations.add(AggregationProvider.getServiceLanguageAggregation());
    return appliedAggregations;
  }

}
