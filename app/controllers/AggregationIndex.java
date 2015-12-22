package controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import helpers.JsonLdConstants;
import models.Resource;
import models.ResourceList;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import play.mvc.Result;
import services.AggregationProvider;
import services.ElasticsearchProvider;

/**
 * @author fo
 */
public class AggregationIndex extends OERWorldMap {

  public static Result list() throws IOException {

    Resource countryAggregation = mBaseRepository.aggregate(AggregationProvider.getByCountryAggregation());
    Resource typeAggregation = mBaseRepository.aggregate(AggregationProvider.getTypeAggregation());
    Resource languageAggregation = mBaseRepository.aggregate(AggregationProvider.getServiceLanguageAggregation());

    ResourceList topLevelISCEDConcepts = mBaseRepository.query(
      "about.topConceptOf.@id:\"https://w3id.org/isced/1997/scheme\"", 0, Integer.MAX_VALUE, null, null);
    ArrayList<String> topLevelISCEDConceptIds = new ArrayList<>();
    for (Resource r : topLevelISCEDConcepts.getItems()) {
      topLevelISCEDConceptIds.add(r.getAsString(JsonLdConstants.ID));
    }
    Resource gradeLevelAggregation = mBaseRepository.aggregate(AggregationProvider.getServiceByGradeLevelAggregation(topLevelISCEDConceptIds));

    ResourceList topLevelESCConcepts = mBaseRepository.query(
      "about.topConceptOf.@id:\"https://w3id.org/class/esc/scheme\"", 0, Integer.MAX_VALUE, null, null);
    ArrayList<String> topLevelESCConceptIds = new ArrayList<>();
    for (Resource r : topLevelESCConcepts.getItems()) {
      topLevelESCConceptIds.add(r.getAsString(JsonLdConstants.ID));
    }
    Resource fieldOfEducationAggregation = mBaseRepository.aggregate(AggregationProvider.getServiceByFieldOfEducationAggregation(topLevelESCConceptIds));

    Map<String,Object> scope = new HashMap<>();
    scope.put("countryAggregation", countryAggregation);
    scope.put("typeAggregation", typeAggregation);
    scope.put("gradeLevelAggregation", gradeLevelAggregation);
    scope.put("languageAggregation", languageAggregation);
    scope.put("fieldOfEducationAggregation", fieldOfEducationAggregation);


    return ok(render("Country Aggregations", "AggregationIndex/index.mustache", scope));

  }

}
