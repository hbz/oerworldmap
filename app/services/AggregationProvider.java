package services;

import helpers.JsonLdConstants;
import models.Record;
import models.Resource;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.sort.SortOrder;

import java.util.Arrays;
import java.util.List;

/**
 * @author fo
 */
public class AggregationProvider {

  public static AggregationBuilder<?> getTypeAggregation(int aSize) {
    return AggregationBuilders.terms("about.@type").size(aSize).field("about.@type").minDocCount(0)
        .exclude("Concept|ConceptScheme|Comment|LikeAction");
  }

  public static AggregationBuilder<?> getLocationAggregation(int aSize) {
    return AggregationBuilders.terms("about.location.address.addressCountry").size(aSize)
        .field("about.location.address.addressCountry");
  }

  public static AggregationBuilder<?> getServiceLanguageAggregation(int aSize) {
    return AggregationBuilders.terms("about.availableChannel.availableLanguage").size(aSize)
        .field("about.availableChannel.availableLanguage");
  }

  public static AggregationBuilder<?> getServiceByFieldOfEducationAggregation(int aSize) {
    return AggregationBuilders.terms("about.about.@id").size(aSize).field("about.about.@id");
  }

  public static AggregationBuilder<?> getServiceByFieldOfEducationAggregation(List<String> anIdList, int aSize) {
    return AggregationBuilders.terms("about.about.@id").size(aSize)
        .field("about.about.@id")
        .include(StringUtils.join(anIdList, '|'));
  }

  public static AggregationBuilder<?> getServiceByTopLevelFieldOfEducationAggregation() {
    String[] topLevelIds = new String[] {
      "https://w3id.org/class/esc/n00",
      "https://w3id.org/class/esc/n01",
      "https://w3id.org/class/esc/n02",
      "https://w3id.org/class/esc/n03",
      "https://w3id.org/class/esc/n04",
      "https://w3id.org/class/esc/n05",
      "https://w3id.org/class/esc/n06",
      "https://w3id.org/class/esc/n07",
      "https://w3id.org/class/esc/n08",
      "https://w3id.org/class/esc/n09",
      "https://w3id.org/class/esc/n10"
    };
    return getServiceByFieldOfEducationAggregation(Arrays.asList(topLevelIds), 0);
  }

  public static AggregationBuilder<?> getServiceByGradeLevelAggregation(int aSize) {
    return AggregationBuilders.terms("about.audience.@id").size(aSize)
        .field("about.audience.@id");
  }

  public static AggregationBuilder<?> getKeywordsAggregation(int aSize) {
    return AggregationBuilders.terms("about.keywords").size(aSize)
      .field("about.keywords");
  }

  public static AggregationBuilder<?> getServiceByGradeLevelAggregation(List<String> anIdList, int aSize) {
    return AggregationBuilders.terms("about.audience.@id").size(aSize)
        .field("about.audience.@id")
        .include(StringUtils.join(anIdList, '|'));
  }

  public static AggregationBuilder<?> getByCountryAggregation(int aSize) {
    return AggregationBuilders
        .terms("about.location.address.addressCountry").field("about.location.address.addressCountry").size(aSize)
        .subAggregation(AggregationBuilders.terms("by_type").field("about.@type"))
        .subAggregation(AggregationBuilders
          .filter("champions")
          .filter(QueryBuilders.existsQuery(Record.RESOURCE_KEY + ".countryChampionFor")));
  }

  public static AggregationBuilder<?> getForCountryAggregation(String aId, int aSize) {
    return AggregationBuilders
        .terms("about.location.address.addressCountry").field("about.location.address.addressCountry").include(aId)
        .size(aSize)
        .subAggregation(AggregationBuilders.terms("by_type").field("about.@type"))
        .subAggregation(AggregationBuilders
            .filter("champions")
            .filter(QueryBuilders.existsQuery(Record.RESOURCE_KEY + ".countryChampionFor")));
  }

  public static AggregationBuilder<?> getNestedConceptAggregation(Resource aConcept, String aField) {
    String id = aConcept.getAsString(JsonLdConstants.ID);
    AggregationBuilder conceptAggregation = AggregationBuilders.filter(id).filter(
      QueryBuilders.termQuery(aField, id)
    );
    for (Resource aNarrowerConcept : aConcept.getAsList("narrower")) {
      conceptAggregation.subAggregation(getNestedConceptAggregation(aNarrowerConcept, aField));
    }
    return conceptAggregation;
  }

  public static AggregationBuilder<?> getLicenseAggregation(int aSize) {
    return AggregationBuilders.terms("about.license.@id").size(aSize)
      .field("about.license.@id");
  }

  public static AggregationBuilder<?> getProjectByLocationAggregation(int aSize) {
    return AggregationBuilders.terms("about.agent.location.address.addressCountry").size(aSize)
      .field("about.agent.location.address.addressCountry");
  }

  public static AggregationBuilder<?> getFunderAggregation(int aSize) {
    return AggregationBuilders.terms("about.isFundedBy.isAwardedBy.@id").size(aSize)
      .field("about.isFundedBy.isAwardedBy.@id");
  }

  public static AggregationBuilder<?> getEventCalendarAggregation() {
    return AggregationBuilders
        .dateHistogram("about.startDate.GTE")
        .field("about.startDate")
        .interval(DateHistogramInterval.MONTH).subAggregation(AggregationBuilders.topHits("about.@id")
            .setFetchSource(new String[]{"about.@id", "about.@type", "about.name", "about.startDate", "about.endDate",
              "about.location"}, null)
          .addSort("about.startDate", SortOrder.ASC).setSize(Integer.MAX_VALUE)
          ).order(Histogram.Order.KEY_DESC);
  }

  public static AggregationBuilder<?> getLikeAggregation(int aSize) {
    return AggregationBuilders.terms("about.object.@id").size(aSize)
      .field("about.object.@id");
  }

}
