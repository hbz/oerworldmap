package controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import models.Record;
import models.Resource;

import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import play.mvc.Result;
import services.AggregationProvider;

import play.mvc.Result;

/**
 * @author fo
 */
public class AggregationIndex extends OERWorldMap {

  public static Result list() throws IOException {

    Resource countryAggregation = mBaseRepository.aggregate(AggregationProvider.getByCountryAggregation());
    Resource typeAggregation = mBaseRepository.aggregate(AggregationProvider.getTypeAggregation());
    Resource gradeLevelAggregation = mBaseRepository.aggregate(AggregationProvider.getServiceByGradeLevelAggregation());
    Resource languageAggregation = mBaseRepository.aggregate(AggregationProvider.getServiceLanguageAggregation());
    Resource fieldOfEducationAggregation = mBaseRepository
        .aggregate(AggregationProvider.getServiceByFieldOfEducationAggregation());

    Map<String,Object> scope = new HashMap<>();
    scope.put("countryAggregation", countryAggregation);
    scope.put("typeAggregation", typeAggregation);
    scope.put("gradeLevelAggregation", gradeLevelAggregation);
    scope.put("languageAggregation", languageAggregation);
    scope.put("fieldOfEducationAggregation", fieldOfEducationAggregation);


    return ok(render("Country Aggregations", "AggregationIndex/index.mustache", scope));

  }

}
