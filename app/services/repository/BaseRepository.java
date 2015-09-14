package services.repository;

import com.typesafe.config.Config;
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

import models.ResourceList;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.json.simple.parser.ParseException;

import play.Logger;
import services.ElasticsearchProvider;
import services.repository.ElasticsearchRepository;
import services.repository.FileRepository;

public class BaseRepository extends Repository implements Readable, Writable, Queryable, Aggregatable {

  private static ElasticsearchRepository mElasticsearchRepo;
  private static FileRepository mFileRepo;

  public BaseRepository(Config aConfiguration) {
    super(aConfiguration);
    mElasticsearchRepo = new ElasticsearchRepository(aConfiguration);
    mFileRepo = new FileRepository(aConfiguration);
  }

  @Override
  public Resource deleteResource(@Nonnull String aId) {
    if (null == mElasticsearchRepo.deleteResource(aId + "." + Record.RESOURCEKEY)) {
      return null;
    } else {
      return mFileRepo.deleteResource(aId + "." + Record.RESOURCEKEY);
    }
  }

  public void addResource(Resource aResource) throws IOException {
    String type = aResource.get(JsonLdConstants.TYPE).toString();
    addResource(aResource, type);
  }

  @Override
  public void addResource(@Nonnull Resource aResource, @Nonnull String aType) throws IOException {
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
    mElasticsearchRepo.addResource(record, aType);
    mFileRepo.addResource(record, aType);
    addMentionedData(aResource);
  }

  /**
   * Add data mentioned within other items.
   *
   * @param aResource
   *          The type of mentioned data sub item.
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
      if (!id.equals(refId)) {
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
      resources.addAll(getResources(mElasticsearchRepo.query(aEsQuery, 0, 9999, null).getItems()));
    } catch (IOException | ParseException e) {
      Logger.error(e.toString());
    }
    return resources;
  }

  @Override
  public ResourceList query(@Nonnull String aQueryString, int aFrom, int aSize, String aSortOrder) {
    try {
      ResourceList resourceList = mElasticsearchRepo.query(aQueryString, aFrom, aSize, aSortOrder);
      List<Resource> resources = new ArrayList<>();
      resources.addAll(getResources(resourceList.getItems()));
      attachReferencedResources(resources);
      resourceList.setItems(resources);
      return resourceList;
    } catch (IOException | ParseException e) {
      Logger.error(e.toString());
      return null;
    }
  }

  @Override
  public Resource getResource(@Nonnull String aId) {
    Resource resource = mElasticsearchRepo.getResource(aId + "." + Record.RESOURCEKEY);
    if (resource == null || resource.isEmpty()) {
      resource = mFileRepo.getResource(aId + "." + Record.RESOURCEKEY);
    }
    if (resource != null) {
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

  @Override
  public Resource aggregate(@Nonnull AggregationBuilder<?> aAggregationBuilder) throws IOException {
    return mElasticsearchRepo.aggregate(aAggregationBuilder);
  }

  @Override
  public List<Resource> getAll(@Nonnull String aType) {
    List<Resource> resources = new ArrayList<Resource>();
    try {
      resources = mElasticsearchRepo.getAll(aType);
    } catch (IOException e) {
      Logger.error(e.toString());
    }
    attachReferencedResources(resources);
    return resources;
  }

}
