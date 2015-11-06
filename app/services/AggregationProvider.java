package services;

import helpers.JsonLdConstants;
import models.Resource;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;

import models.Record;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author fo
 */
public class AggregationProvider {

  public static AggregationBuilder<?> getTypeAggregation() {
    return AggregationBuilders.terms("about.@type").size(0).field("about.@type");
  }

  public static AggregationBuilder<?> getLocationAggregation() {
    return AggregationBuilders.terms("about.location.address.addressCountry").size(0)
        .field("about.location.address.addressCountry");
  }

  public static AggregationBuilder<?> getServiceLanguageAggregation() {
    return AggregationBuilders.terms("about.availableChannel.availableLanguage").size(0)
        .field("about.availableChannel.availableLanguage");
  }

  public static AggregationBuilder<?> getServiceByFieldOfEducationAggregation() {
    return AggregationBuilders.terms("about.about.@id").size(0).field("about.about.@id");
  }

  public static AggregationBuilder<?> getServiceByFieldOfEducationAggregation(List<String> anIdList) {
    return AggregationBuilders.terms("about.about.@id").size(0)
        .field("about.about.@id")
        .include(StringUtils.join(anIdList, '|'));
  }

  public static AggregationBuilder<?> getServiceByGradeLevelAggregation() {
    return AggregationBuilders.terms("about.audience.@id").size(0)
        .field("about.audience.@id");
  }

  public static AggregationBuilder<?> getServiceByGradeLevelAggregation(List<String> anIdList) {
    return AggregationBuilders.terms("about.audience.@id").size(0)
        .field("about.audience.@id")
        .include(StringUtils.join(anIdList, '|'));
  }

  public static AggregationBuilder<?> getByCountryAggregation() {
    return AggregationBuilders
        .terms("about.location.address.addressCountry").field("about.location.address.addressCountry").size(0)
        .subAggregation(AggregationBuilders.terms("by_type").field("about.@type"))
        .subAggregation(AggregationBuilders
            .filter("champions")
            .filter(FilterBuilders.existsFilter(Record.RESOURCEKEY + ".countryChampionFor")));
  }

  public static AggregationBuilder<?> getForCountryAggregation(String id) {
    return AggregationBuilders
        .terms("about.location.address.addressCountry").field("about.location.address.addressCountry").include(id).size(0)
        .subAggregation(AggregationBuilders.terms("by_type").field("about.@type"))
        .subAggregation(AggregationBuilders
            .filter("champions")
            .filter(FilterBuilders.existsFilter(Record.RESOURCEKEY + ".countryChampionFor")));
  }

  public static AggregationBuilder<?> getNestedConceptAggregation(Resource aConcept, String aField) {
    String id = aConcept.getAsString(JsonLdConstants.ID);
    AggregationBuilder conceptAggregation = AggregationBuilders.filter(id).filter(
      FilterBuilders.termFilter(aField, id)
    );
    for (Resource aNarrowerConcept : aConcept.getAsList("narrower")) {
      conceptAggregation.subAggregation(getNestedConceptAggregation(aNarrowerConcept, aField));
    }
    return conceptAggregation;
  }

}
