package services;

import models.Record;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.terms.IncludeExclude;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.util.Arrays;
import java.util.List;

/**
 * @author fo
 */
public class AggregationProvider {

  private static int getSize(int aSize) {
    return aSize == 0 ? Integer.MAX_VALUE : aSize;
  }

  public static AggregationBuilder getTypeAggregation(int aSize) {
    return AggregationBuilders.terms("about.@type").size(getSize(aSize)).field("about.@type")
      .minDocCount(0)
      .includeExclude(
        new IncludeExclude(null, "Concept|ConceptScheme|Comment|LikeAction|LighthouseAction"))
      .order(BucketOrder.key(false));
  }

  public static AggregationBuilder getServiceLanguageAggregation(int aSize) {
    return AggregationBuilders.terms("about.availableChannel.availableLanguage")
      .size(getSize(aSize))
      .field("about.availableChannel.availableLanguage");
  }

  public static AggregationBuilder getServiceByFieldOfEducationAggregation(int aSize) {
    return AggregationBuilders.terms("about.about.@id").size(getSize(aSize))
      .field("about.about.@id");
  }

  public static AggregationBuilder getServiceByFieldOfEducationAggregation(List<String> anIdList,
    int aSize) {
    return AggregationBuilders.terms("about.about.@id").size(getSize(aSize))
      .field("about.about.@id")
      .includeExclude(new IncludeExclude(StringUtils.join(anIdList, '|'), null));
  }

  public static AggregationBuilder getServiceByTopLevelFieldOfEducationAggregation() {
    String[] topLevelIds = new String[]{
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
    return getServiceByFieldOfEducationAggregation(Arrays.asList(topLevelIds), 1);
  }

  public static AggregationBuilder getServiceByGradeLevelAggregation(int aSize) {
    return AggregationBuilders.terms("about.audience.@id").size(getSize(aSize))
      .field("about.audience.@id");
  }

  public static AggregationBuilder getKeywordsAggregation(int aSize) {
    return AggregationBuilders.terms("about.keywords").size(getSize(aSize))
      .field("about.keywords");
  }


  public static AggregationBuilder getByCountryAggregation(int aSize) {
    return AggregationBuilders
      .terms("feature.properties.location.address.addressCountry")
      .field("feature.properties.location.address.addressCountry").size(getSize(aSize))
      .subAggregation(AggregationBuilders.terms("by_type").field("about.@type").minDocCount(0));
  }


  public static AggregationBuilder getForCountryAggregation(String aId, int aSize) {
    return AggregationBuilders.filter("country",
      QueryBuilders.termQuery("feature.properties.location.address.addressCountry", aId))
      .subAggregation(AggregationBuilders.terms("by_type").field("about.@type").minDocCount(0)
        .includeExclude(
          new IncludeExclude(null,"Concept|ConceptScheme|Comment|LikeAction|LighthouseAction")))
      .subAggregation(AggregationBuilders
        .filter("reports", QueryBuilders
          .matchQuery(Record.RESOURCE_KEY + ".keywords", "countryreport:".concat(aId)))
        .subAggregation(AggregationBuilders.topHits("country_reports")));
  }


  public static AggregationBuilder getLicenseAggregation(int aSize) {
    return AggregationBuilders.terms("about.license.@id").size(getSize(aSize))
      .field("about.license.@id");
  }


  public static AggregationBuilder getProjectByLocationAggregation(int aSize) {
    return AggregationBuilders.terms("about.agent.location.address.addressCountry")
      .size(getSize(aSize))
      .field("about.agent.location.address.addressCountry");
  }


  public static AggregationBuilder getFunderAggregation(int aSize) {
    return AggregationBuilders.terms("about.isFundedBy.isAwardedBy.@id").size(getSize(aSize))
      .field("about.isFundedBy.isAwardedBy.@id");
  }


  public static AggregationBuilder getEventCalendarAggregation() {
    return AggregationBuilders
      .dateHistogram("about.startDate.GTE")
      .field("about.startDate")
      .dateHistogramInterval(DateHistogramInterval.MONTH)
      .subAggregation(AggregationBuilders.topHits("about.@id")
        .fetchSource(
          new String[]{"about.@id", "about.@type", "about.name", "about.startDate", "about.endDate",
            "about.location"}, null)
        .sort(new FieldSortBuilder("about.startDate").order(SortOrder.ASC).unmappedType("string"))
        .size(100)
      ).order(BucketOrder.key(true)).minDocCount(1);
  }


  public static AggregationBuilder getRegionAggregation(int aSize, String aIso3166Scope) {
    return AggregationBuilders
      .terms("feature.properties.location.address.addressRegion")
      .field("feature.properties.location.address.addressRegion")
      .includeExclude(new IncludeExclude(aIso3166Scope + "\\...+", null))
      .size(getSize(aSize))
      .subAggregation(AggregationBuilders.terms("by_type").field("about.@type").minDocCount(0)
        .includeExclude(
          new IncludeExclude(null,"Concept|ConceptScheme|Comment|LikeAction|LighthouseAction")));
  }

  public static AggregationBuilder getLikeAggregation(int aSize) {
    return AggregationBuilders.terms("about.object.@id").size(getSize(aSize))
      .field("about.object.@id");
  }

  public static AggregationBuilder getPrimarySectorsAggregation(int aSize) {
    return AggregationBuilders.terms("about.primarySector.@id").size(getSize(aSize))
      .field("about.primarySector.@id");
  }

  public static AggregationBuilder getSecondarySectorsAggregation(int aSize) {
    return AggregationBuilders.terms("about.secondarySector.@id").size(getSize(aSize))
      .field("about.secondarySector.@id");
  }

  public static AggregationBuilder getAwardAggregation(int aSize) {
    return AggregationBuilders.terms("about.award").size(getSize(aSize))
      .field("about.award");
  }

  public static AggregationBuilder getFieldOfActivityAggregation(int aSize) {
    return AggregationBuilders.terms("about.activityField.@id").size(getSize(aSize))
      .field("about.activityField.@id");
  }

  public static AggregationBuilder getChampionsAggregation(int aSize) {
    return AggregationBuilders.global("champions")
      .subAggregation(AggregationBuilders.terms("about.countryChampionFor.keyword").size(getSize(aSize))
        .field("about.countryChampionFor.keyword")
        .subAggregation(AggregationBuilders.topHits("country_champions")))
      .subAggregation(AggregationBuilders.terms("about.regionalChampionFor.keyword").size(getSize(aSize))
        .field("about.regionalChampionFor.keyword")
        .subAggregation(AggregationBuilders.topHits("regional_champions")));
  }

}
