package services;

import controllers.Global;
import controllers.Secured;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import play.mvc.Http.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fo
 */
public class QueryContext {

  private Context mContext;

  public QueryContext(Context context) {
    mContext = context;
  }

  public List<FilterBuilder> getFilters() {

    List<FilterBuilder> filters = new ArrayList<>();

    filters.add(FilterBuilders.notFilter(
      FilterBuilders.orFilter(
        FilterBuilders.termFilter("about.@type", "Concept"),
        FilterBuilders.termFilter("about.@type", "ConceptScheme")
      )
    ));

    // TODO: Remove privacy filter when all persons are accounts?
    if (!Global.getConfig().getString("admin.user").equals(Secured.getHttpBasicAuthUser(mContext))) {
      filters.add(FilterBuilders.notFilter(FilterBuilders.andFilter(FilterBuilders
        .termFilter("about.@type", "Person"), FilterBuilders.notFilter(FilterBuilders.existsFilter("about.email")))));
    }

    return filters;

  }

  public List<AggregationBuilder> getAggregations() {

    List<AggregationBuilder> aggregations = new ArrayList<>();
    aggregations.add(AggregationProvider.getTypeAggregation());
    aggregations.add(AggregationProvider.getByCountryAggregation());
    aggregations.add(AggregationProvider.getServiceLanguageAggregation());
    //aggregations.add(AggregationProvider.getFieldOfEducationAggregation());

    return aggregations;

  }

}
