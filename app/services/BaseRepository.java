package services;

import helpers.JsonLdConstants;
import helpers.UniversalFunctions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import models.Record;
import models.Resource;

import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.json.simple.parser.ParseException;

import play.Logger;

public class BaseRepository implements ResourceRepository {

  private static ElasticsearchRepository mElasticsearchRepo;
  private static FileResourceRepository mFileRepo;

  public BaseRepository(ElasticsearchProvider aElasticsearchProvider, Path aPath) throws IOException {
    mElasticsearchRepo = new ElasticsearchRepository(aElasticsearchProvider);
    mFileRepo = new FileResourceRepository(aPath);
  }

  public Resource deleteResource(String aId) {
    if (null == mElasticsearchRepo.deleteResource(aId + "." + Record.RESOURCEKEY)) {
      return null;
    } else {
      return mFileRepo.deleteResource(aId + "." + Record.RESOURCEKEY);
    }
  }

  public void addResource(Resource aResource) throws IOException {
    List<Resource> denormalizedResources = ResourceDenormalizer.denormalize(aResource, this);
    for (Resource dnr : denormalizedResources) {
      if (dnr.hasId()) {
        Resource rec = getRecord(dnr);
        mElasticsearchRepo.addResource(rec);
        mFileRepo.addResource(rec);
      }
    }
  }

  public List<Resource> esQuery(String aEsQuery, String aEsSort) {
    // FIXME: hardcoded access restriction to newsletter-only users, criteria:
    // has no unencrypted email address
    aEsQuery += " AND ((about.@type:Article OR about.@type:Organization OR about.@type:Action OR about.@type:Service) OR (about.@type:Person AND about.email:*))";
    List<Resource> resources = new ArrayList<Resource>();
    try {
      resources.addAll(getResources(mElasticsearchRepo.esQuery(aEsQuery, aEsSort)));
    } catch (IOException | ParseException e) {
      Logger.error(e.toString());
    }
    return resources;
    // TODO eventually add FileResourceRepository.esQuery(String aEsQuery)
  }

  public Resource getResource(String aId) {
    
    Resource resource = mElasticsearchRepo.getResource(aId + "." + Record.RESOURCEKEY);
    if (resource == null || resource.isEmpty()) {
      resource = mFileRepo.getResource(aId + "." + Record.RESOURCEKEY);
    }
    if (resource != null) {
      resource = (Resource) resource.get(Record.RESOURCEKEY);
    }
    return resource;
  }

  private Resource getRecord(Resource aResource) {
    String id = (String) aResource.get(JsonLdConstants.ID);
    Resource record;
    if (null != id) {
      record = getRecordFromRepo(id);
      if (null == record) {
        record = new Record(aResource);
      } else {
        record.put("dateModified", UniversalFunctions.getCurrentTime());
        record.put(Record.RESOURCEKEY, aResource);
      }
    } else {
      record = new Record(aResource);
    }
    return record;
  }

  private Resource getRecordFromRepo(String aId) {
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

  public Resource query(AggregationBuilder<?> aAggregationBuilder) throws IOException {
    return mElasticsearchRepo.query(aAggregationBuilder);
  }

  public Resource query(List<AggregationBuilder<?>> aAggregationBuilders) throws IOException {
    return mElasticsearchRepo.query(aAggregationBuilders);
  }

  public List<Resource> query(String aType, boolean aSearchAllRepos) throws IOException {
    List<Resource> resources = new ArrayList<Resource>();
    resources.addAll(getResources(mElasticsearchRepo.query(aType)));
    if (aSearchAllRepos || resources.isEmpty()) {
      resources.addAll(mFileRepo.query(aType));
    }
    return resources;
  }

  @Override
  public List<Resource> query(String aType) throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Resource> getResourcesByContent(String aType, String aField, String aContent) {
    // TODO Auto-generated method stub
    return null;
  }
}
