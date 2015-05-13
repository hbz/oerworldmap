package services;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import helpers.JsonLdConstants;
import helpers.UniversalFunctions;
import models.Record;
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
    String id = (String) aResource.get(JsonLdConstants.ID);
    Resource record;
    if (null != id) {
      record = getRecord(id);
      if (null == record) {
        record = new Record(aResource);
      } else {
        record.put("dateModified", UniversalFunctions.getCurrentTime());
        record.put(Record.RESOURCEKEY, aResource);
      }
    } else {
      record = new Record(aResource);
    }
    mElasticsearchRepo.addResource(record, aResource.get(JsonLdConstants.TYPE).toString());
    mFileRepo.addResource(record);
    addMentionedData(aResource);
  }

  /**
   * Add data mentioned within other items.
   * 
   * @param aResource The type of mentioned data sub item.
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

  public Resource getResource(String aId) {
    Resource resource = mElasticsearchRepo.getResource(aId + "." + Record.RESOURCEKEY);
    if (resource == null || resource.isEmpty()) {
      resource = mFileRepo.getResource(aId + "." + Record.RESOURCEKEY);
    }
    if (resource != null){
      resource = (Resource) resource.get(Record.RESOURCEKEY);
    }
    return resource;
  }

  private Resource getRecord(String aId) {
    Resource record = mElasticsearchRepo.getResource(aId + "." + Record.RESOURCEKEY);
    if (record == null || record.isEmpty()) {
      record = mFileRepo.getResource(aId + "." + Record.RESOURCEKEY);
    }
    return record;
  }

  private List<Resource> getResources(List<Resource> aRecords) {
    List<Resource> resources = new ArrayList<Resource>();
    for (Resource rec : aRecords) {
      resources.add((Resource) rec.get(Record.RESOURCEKEY));
    }
    return resources;
  }

  public List<Resource> getResourcesByContent(String aType, String aField, String aContent,
      boolean aSearchAllRepos) {
    String field = Record.RESOURCEKEY + "." + aField;
    List<Resource> resources = new ArrayList<Resource>();
    resources
        .addAll(getResources(mElasticsearchRepo.getResourcesByContent(aType, field, aContent)));
    if (aSearchAllRepos || resources.isEmpty()) {
      resources.addAll(mFileRepo.getResourcesByContent(aType, field, aContent));
    }
    return resources;
  }

  public Resource query(@SuppressWarnings("rawtypes") AggregationBuilder aAggregationBuilder)
      throws IOException {
    return mElasticsearchRepo.query(aAggregationBuilder);
  }

  public List<Resource> query(String aType, boolean aSearchAllRepos) throws IOException {
    List<Resource> resources = new ArrayList<Resource>();
    resources.addAll(getResources(mElasticsearchRepo.query(aType)));
    if (aSearchAllRepos || resources.isEmpty()) {
      resources.addAll(mFileRepo.query(aType));
    }
    return resources;
  }
}
