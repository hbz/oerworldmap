package services;

import helpers.JsonLdConstants;
import helpers.UniversalFunctions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import models.Record;
import models.Resource;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.json.simple.parser.ParseException;

import play.Logger;

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

  private void attachReferencedResources(Resource aResource) {
    String id = aResource.get(JsonLdConstants.ID).toString();
    List<Resource> referencedResources = esQueryNoRef(QueryParser.escape(id));
    List<Resource> referencedResourcesExludingSelf = new ArrayList<>();
    for (Resource reference : referencedResources) {
      String refId = reference.get(JsonLdConstants.ID).toString();
      if (! id.equals(refId)) {
        referencedResourcesExludingSelf.add(reference);
      }
    }
    if (!referencedResourcesExludingSelf.isEmpty()) {
      aResource.put(Resource.REFERENCEKEY, referencedResourcesExludingSelf);
    }
  }

  private void attachReferencedResources(List<Resource> aResources) {
    for (Resource resource : aResources) {
      attachReferencedResources(resource);
    }
  }

  private List<Resource> esQueryNoRef(String aEsQuery) {
    List<Resource> resources = new ArrayList<Resource>();
    try {
      resources.addAll(getResources(mElasticsearchRepo.esQuery(aEsQuery)));
    } catch (IOException | ParseException e) {
      Logger.error(e.toString());
    }
    return resources;
  }

  public List<Resource> esQuery(String aEsQuery) {
    // FIXME: hardcoded access restriction to newsletter-only unsers, criteria: has no unencrypted email address
    aEsQuery += " AND ((about.@type:Article OR about.@type:Organization OR about.@type:Action OR about.@type:Service) OR (about.@type:Person AND about.email:*))";
    List<Resource> resources = new ArrayList<Resource>();
    try {
      resources.addAll(getResources(mElasticsearchRepo.esQuery(aEsQuery)));
      attachReferencedResources(resources);
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
    if (resource != null){
      resource = (Resource) resource.get(Record.RESOURCEKEY);
      attachReferencedResources(resource);
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
    attachReferencedResources(resources);
    return resources;
  }

  public Resource query(@SuppressWarnings("rawtypes") AggregationBuilder aAggregationBuilder)
      throws IOException {
    return mElasticsearchRepo.query(aAggregationBuilder);
  }

  public Resource query(@SuppressWarnings("rawtypes") List<AggregationBuilder> aAggregationBuilders)
      throws IOException {
    return mElasticsearchRepo.query(aAggregationBuilders);
  }

  public List<Resource> query(String aType, boolean aSearchAllRepos) throws IOException {
    List<Resource> resources = new ArrayList<Resource>();
    resources.addAll(getResources(mElasticsearchRepo.query(aType)));
    if (aSearchAllRepos || resources.isEmpty()) {
      resources.addAll(mFileRepo.query(aType));
    }
    attachReferencedResources(resources);
    return resources;
  }
}
