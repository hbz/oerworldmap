package services;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import models.Resource;

import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.json.simple.parser.ParseException;

import com.fasterxml.jackson.databind.JsonNode;

public class BaseRepository {

  private static ElasticsearchRepository mElasticsearchRepo;
  private static FileResourceRepository mFileRepo;

  public BaseRepository(ElasticsearchClient aElasticsearchClient, Path aPath) throws IOException {
    mElasticsearchRepo = new ElasticsearchRepository(aElasticsearchClient);
    mFileRepo = new FileResourceRepository(aPath);
  }

  public Resource deleteResource(String aId) throws IOException {
    // TODO: add ElasticsearchRepository.deleteResource(String aId)
    return mFileRepo.deleteResource(aId);
  }

  public void addResource(Resource aResource) throws IOException {
    mElasticsearchRepo.addResource(aResource);
    mFileRepo.addResource(aResource);
    addMentionedData(aResource);
  }

  /**
   * Add data mentioned within other items.
   * 
   * @param aType The type of mentioned data sub item.
   * @param aContent The data content of mentioned data sub item.
   * @throws IOException
   */
  @SuppressWarnings("unchecked")
  private void addMentionedData(@Nonnull final Resource aResource) throws IOException {
    for (Map.Entry<String, Object> entry : aResource.entrySet()) {
      Object value = entry.getValue();
      if (value instanceof Resource) {
        Resource r = (Resource) value;
        if (r.hasId()) {
          addResource(((Resource) value));
        }
      } else if (value instanceof List) {
        for (Object v : (List<Object>) value) {
          if (v instanceof Resource) {
            Resource r = (Resource) v;
            if (r.hasId()) {
              addResource(r);
            }
          }
        }
      }
    }
  }

  public List<Resource> esQuery(String aEsQuery) throws IOException, ParseException {
    return mElasticsearchRepo.esQuery(aEsQuery);
    // TODO eventually add FileResourceRepository.esQuery(String aEsQuery)
  }

  public Resource getResource(String aId) throws IOException {
    Resource resource = mElasticsearchRepo.getResource(aId);
    if (resource == null || resource.isEmpty()) {
      resource = mFileRepo.getResource(aId);
    }
    return resource;
  }

  public List<Resource> getResourcesByContent(String aType, String aField, String aContent,
      boolean aSearchAllRepos) {
    List<Resource> resources = new ArrayList<Resource>();
    resources.addAll(mElasticsearchRepo.getResourcesByContent(aType, aField, aContent));
    if (aSearchAllRepos || resources == null || resources.isEmpty()) {
      resources.addAll(mFileRepo.getResourcesByContent(aType, aField, aContent));
    }
    return resources;
  }

  public Resource query(@SuppressWarnings("rawtypes") AggregationBuilder aAggregationBuilder)
      throws IOException {
    return mElasticsearchRepo.query(aAggregationBuilder);
  }

  public List<Resource> query(String aType, boolean aSearchAllRepos) throws IOException {
    List<Resource> resources = new ArrayList<Resource>();
    resources.addAll(mElasticsearchRepo.query(aType));
    if (aSearchAllRepos || resources == null || resources.isEmpty()) {
      resources.addAll(mFileRepo.query(aType));
    }
    return resources;
  }
}
