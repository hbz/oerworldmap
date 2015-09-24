package services;

import models.Record;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;

import java.util.HashMap;
import java.util.Map;

/**
 * @author fo
 */
public class AggregationProvider {

  public static AggregationBuilder<?> getTypeAggregation() {
    return AggregationBuilders.terms("about.@type").field("about.@type");
  }

  public static AggregationBuilder<?> getLocationAggregation() {
    return AggregationBuilders.terms("about.location.address.addressCountry")
        .field("about.location.address.addressCountry");
  }

  public static AggregationBuilder<?> getServiceLanguageAggregation() {
    return AggregationBuilders.terms("about.availableChannel.availableLanguage")
        .field("about.availableChannel.availableLanguage");
  }

  public static AggregationBuilder<?> getServiceByFieldOfEducationAggregation() {
    return AggregationBuilders.terms("about.about.@id")
        .field("about.about.@id");
  }

  public static AggregationBuilder<?> getServiceByGradeLevelAggregation() {
    return AggregationBuilders.terms("about.audience.@id")
        .field("about.audience.@id");
  }

  public static AggregationBuilder<?> getByCountryAggregation() {
    return AggregationBuilders
        .terms("about.location.address.addressCountry").field("about.location.address.addressCountry").size(0)
        .subAggregation(AggregationBuilders.terms("by_type").field("about.@type").minDocCount(0))
        .subAggregation(AggregationBuilders
            .filter("champions")
            .filter(FilterBuilders.existsFilter(Record.RESOURCEKEY + ".countryChampionFor")));
  }

  public static AggregationBuilder<?> getForCountryAggregation(String id) {
    return AggregationBuilders
        .terms("about.location.address.addressCountry").field("about.location.address.addressCountry").include(id).size(0)
        .subAggregation(AggregationBuilders.terms("by_type").field("about.@type").minDocCount(0))
        .subAggregation(AggregationBuilders
            .filter("champions")
            .filter(FilterBuilders.existsFilter(Record.RESOURCEKEY + ".countryChampionFor")));
  }

}
