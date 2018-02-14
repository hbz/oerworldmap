package services;

import helpers.JsonLdConstants;
import models.Record;
import models.Resource;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.terms.IncludeExclude;
import org.elasticsearch.search.sort.SortOrder;

import java.util.Arrays;
import java.util.List;

/**
 * @author fo
 */
public class AggregationProvider {

  public static AggregationBuilder getTypeAggregation(int aSize) {
    return AggregationBuilders.terms("about.@type").size(aSize).field("about.@type").minDocCount(0)
      .includeExclude(new IncludeExclude(null, "Concept|ConceptScheme|Comment|LikeAction|LighthouseAction"));
  }

  public static AggregationBuilder getLocationAggregation(int aSize) {
    return AggregationBuilders.terms("about.location.address.addressCountry").size(aSize)
        .field("about.location.address.addressCountry");
  }

  public static AggregationBuilder getServiceLanguageAggregation(int aSize) {
    return AggregationBuilders.terms("about.availableChannel.availableLanguage").size(aSize)
        .field("about.availableChannel.availableLanguage");
  }

  public static AggregationBuilder getServiceByFieldOfEducationAggregation(int aSize) {
    return AggregationBuilders.terms("about.about.@id").size(aSize).field("about.about.@id");
  }

  public static AggregationBuilder getServiceByFieldOfEducationAggregation(List<String> anIdList, int aSize) {
    return AggregationBuilders.terms("about.about.@id").size(aSize)
        .field("about.about.@id").includeExclude(new IncludeExclude(StringUtils.join(anIdList, '|'), null));
  }

  public static AggregationBuilder getServiceByTopLevelFieldOfEducationAggregation() {
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

  public static AggregationBuilder getServiceByGradeLevelAggregation(int aSize) {
    return AggregationBuilders.terms("about.audience.@id").size(aSize)
        .field("about.audience.@id");
  }

  public static AggregationBuilder getKeywordsAggregation(int aSize) {
    return AggregationBuilders.terms("about.keywords").size(aSize)
      .field("about.keywords");
  }

  public static AggregationBuilder getServiceByGradeLevelAggregation(List<String> anIdList, int aSize) {
    return AggregationBuilders.terms("about.audience.@id").size(aSize)
        .field("about.audience.@id").includeExclude(new IncludeExclude(StringUtils.join(anIdList, '|'), null));
  }

  public static AggregationBuilder getByCountryAggregation(int aSize) {
    return AggregationBuilders
        .terms("about.location.address.addressCountry").field("about.location.address.addressCountry").size(aSize)
        .subAggregation(AggregationBuilders.terms("by_type").field("about.@type"))
        .subAggregation(AggregationBuilders
          .filter("champions", QueryBuilders.existsQuery(Record.RESOURCE_KEY + ".countryChampionFor")));
  }

  public static AggregationBuilder getForCountryAggregation(String aId, int aSize) {
    return AggregationBuilders
        .terms("about.location.address.addressCountry").field("about.location.address.addressCountry")
        .includeExclude(new IncludeExclude(aId, null))
        .size(aSize)
        .subAggregation(AggregationBuilders.terms("by_type").field("about.@type"))
        .subAggregation(AggregationBuilders
            .filter("champions", QueryBuilders.existsQuery(Record.RESOURCE_KEY + ".countryChampionFor")));
  }

  public static AggregationBuilder getNestedConceptAggregation(Resource aConcept, String aField) {
    String id = aConcept.getAsString(JsonLdConstants.ID);
    AggregationBuilder conceptAggregation = AggregationBuilders.filter(id, QueryBuilders.termQuery(aField, id));
    for (Resource aNarrowerConcept : aConcept.getAsList("narrower")) {
      conceptAggregation.subAggregation(getNestedConceptAggregation(aNarrowerConcept, aField));
    }
    return conceptAggregation;
  }

  public static AggregationBuilder getLicenseAggregation(int aSize) {
    return AggregationBuilders.terms("about.license.@id").size(aSize)
      .field("about.license.@id");
  }

  public static AggregationBuilder getProjectByLocationAggregation(int aSize) {
    return AggregationBuilders.terms("about.agent.location.address.addressCountry").size(aSize)
      .field("about.agent.location.address.addressCountry");
  }

  public static AggregationBuilder getFunderAggregation(int aSize) {
    return AggregationBuilders.terms("about.isFundedBy.isAwardedBy.@id").size(aSize)
      .field("about.isFundedBy.isAwardedBy.@id");
  }

  public static AggregationBuilder getEventCalendarAggregation() {
    return AggregationBuilders
        .dateHistogram("about.startDate.GTE")
        .field("about.startDate")
        .dateHistogramInterval(DateHistogramInterval.MONTH).subAggregation(AggregationBuilders.topHits("about.@id")
          .fetchSource(new String[]{"about.@id", "about.@type", "about.name", "about.startDate", "about.endDate",
            "about.location"}, null)
          .sort("about.startDate", SortOrder.ASC).size(Integer.MAX_VALUE)
      ).order(BucketOrder.key(false)).minDocCount(1);
  }

  public static AggregationBuilder getRegionAggregation(int aSize) {
    return AggregationBuilders.terms("about.location.address.addressRegion")
      .field("about.location.address.addressRegion")
      .includeExclude(new IncludeExclude("..\\....?", null))
      .size(aSize);
  }

  public static AggregationBuilder getLikeAggregation(int aSize) {
    return AggregationBuilders.terms("about.object.@id").size(aSize)
      .field("about.object.@id");
  }

  public static AggregationBuilder getPrimarySectorsAggregation(int aSize) {
    return AggregationBuilders.terms("about.primarySector.@id").size(aSize)
      .field("about.primarySector.@id");
  }

  public static AggregationBuilder getSecondarySectorsAggregation(int aSize) {
    return AggregationBuilders.terms("about.secondarySector.@id").size(aSize)
      .field("about.secondarySector.@id");
  }

  public static AggregationBuilder getAwardAggregation(int aSize) {
    return AggregationBuilders.terms("about.award").size(aSize)
      .field("about.award");
  }

}
