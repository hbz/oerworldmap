package services;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import models.Resource;

import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.json.simple.parser.ParseException;

public class BaseRepository {
  
  private static ElasticsearchRepository mElasticsearchRepo;
  private static FileResourceRepository mFileRepo;

  public BaseRepository (ElasticsearchClient aElasticsearchClient, Path aPath) throws IOException {
    mElasticsearchRepo = new ElasticsearchRepository(aElasticsearchClient);
    mFileRepo = new FileResourceRepository(aPath);
  }
  
  public Resource deleteResource(String aId) throws IOException{
    // TODO: add ElasticsearchRepository.deleteResource(String aId)
    return mFileRepo.deleteResource(aId);
  }
  
  public void addResource(Resource aResource) throws IOException{
    mElasticsearchRepo.addResource(aResource);
    mFileRepo.addResource(aResource);
  }
  
  public List<Resource> esQuery(String aEsQuery) throws IOException, ParseException{
    return mElasticsearchRepo.esQuery(aEsQuery);
    // TODO eventually add FileResourceRepository.esQuery(String aEsQuery)
  }
  
  public Resource getResource(String aId) throws IOException{
    Resource resource = mElasticsearchRepo.getResource(aId);
    if (resource == null || resource.isEmpty()){
      resource = mFileRepo.getResource(aId);
    }
    return resource;
  }
  
  public List<Resource> getResourcesByContent(String aType, String aField, String aContent, boolean aSearchAllRepos){
    List<Resource> resources = new ArrayList<Resource>(); 
    resources.addAll(mElasticsearchRepo.getResourcesByContent(aType, aField, aContent));
    if (aSearchAllRepos || resources == null || resources.isEmpty()){
      resources.addAll(mFileRepo.getResourcesByContent(aType, aField, aContent));
    }
    return resources;
  }
  
  public Resource query(@SuppressWarnings("rawtypes") AggregationBuilder aAggregationBuilder) throws IOException{
    return mElasticsearchRepo.query(aAggregationBuilder);
  }
  
  public List<Resource> query(String aType, boolean aSearchAllRepos) throws IOException {
    List<Resource> resources = new ArrayList<Resource>();
    resources.addAll(mElasticsearchRepo.query(aType));
    if (aSearchAllRepos || resources == null || resources.isEmpty()){
      resources.addAll(mFileRepo.query(aType));
    }
    return resources;
  }
}
