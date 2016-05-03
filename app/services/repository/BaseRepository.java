package services.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.json.simple.parser.ParseException;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.typesafe.config.Config;

import helpers.JsonLdConstants;
import helpers.UniversalFunctions;
import models.Record;
import models.Resource;
import models.ResourceList;
import play.Logger;
import services.QueryContext;
import services.ResourceDenormalizer;

public class BaseRepository extends Repository
    implements Readable, Writable, Queryable, Aggregatable {

  private static ElasticsearchRepository mElasticsearchRepo;
  // private static FileRepository mFileRepo;

  public BaseRepository(Config aConfiguration) {
    super(aConfiguration);
    mElasticsearchRepo = new ElasticsearchRepository(aConfiguration);
    // mFileRepo = new FileRepository(aConfiguration);
  }

  @Override
  public Resource deleteResource(@Nonnull String aId) throws IOException {
    // query Resources that mention this Resource and delete the references
    List<Resource> resources = getResources("_all", aId);
    // TODO: find better performing query
    for (Resource resource : resources) {
      if (resource == null || resource.getAsString(JsonLdConstants.ID).equals(aId)) {
        continue;
      }
      String type = resource.getAsString(JsonLdConstants.TYPE);
      resource = resource.removeAllReferencesTo(aId);
      addResource(getRecord(resource), type);
    }

    // delete the resource itself
    Resource result = mElasticsearchRepo.deleteResource(aId + "." + Record.RESOURCEKEY);
    return result;
  }

  public void addResource(Resource aResource) throws IOException {
    List<Resource> denormalizedResources = ResourceDenormalizer.denormalize(aResource, this);
    for (Resource dnr : denormalizedResources) {
      if (dnr.hasId()) {
        Resource rec = getRecord(dnr);
        // Extract the type from the resource, otherwise everything will be
        // typed WebPage!
        String type = dnr.getAsString(JsonLdConstants.TYPE);
        addResource(rec, type);
      }
    }
  }

  @Override
  public void addResource(@Nonnull Resource aResource, @Nonnull String aType) throws IOException {
    mElasticsearchRepo.addResource(aResource, aType);
    // FIXME: As is the case for getResource, this may result in too many open
    // files
    // mFileRepo.addResource(aResource, aType);
  }

  public ProcessingReport validateAndAdd(Resource aResource) throws IOException {
    List<Resource> denormalizedResources = ResourceDenormalizer.denormalize(aResource, this);
    ProcessingReport processingReport = new ListProcessingReport();
    for (Resource dnr : denormalizedResources) {
      try {
        processingReport.mergeWith(dnr.validate());
      } catch (ProcessingException e) {
        Logger.error(e.toString());
      }
    }
    if (!processingReport.isSuccess()) {
      return processingReport;
    }
    for (Resource dnr : denormalizedResources) {
      if (dnr.hasId()) {
        Resource rec = getRecord(dnr);
        // Extract the type from the resource, otherwise everything will be
        // typed WebPage!
        String type = dnr.getAsString(JsonLdConstants.TYPE);
        addResource(rec, type);
      }
    }
    return processingReport;
  }

  @Override
  public ResourceList query(@Nonnull String aQueryString, int aFrom, int aSize, String aSortOrder,
                            Map<String, ArrayList<String>> aFilters) {
    return query(aQueryString, aFrom, aSize, aSortOrder, aFilters, null);
  }

  public ResourceList query(@Nonnull String aQueryString, int aFrom, int aSize, String aSortOrder,
      Map<String, ArrayList<String>> aFilters, QueryContext aQueryContext) {
    ResourceList resourceList;
    try {
      resourceList = mElasticsearchRepo.query(aQueryString, aFrom, aSize, aSortOrder, aFilters, aQueryContext);
    } catch (IOException | ParseException e) {
      Logger.error(e.toString());
      return null;
    }
    // members are Records, unwrap to plain Resources
    List<Resource> resources = new ArrayList<>();
    resources.addAll(unwrapRecords(resourceList.getItems()));
    resourceList.setItems(resources);
    return resourceList;
  }

  @Override
  public Resource getResource(@Nonnull String aId) {
    Resource resource = mElasticsearchRepo.getResource(aId + "." + Record.RESOURCEKEY);
    if (resource == null || resource.isEmpty()) {
      // FIXME: This may lead to inconsistencies (too many open files) when ES
      // and FS are out of sync
      // resource = mFileRepo.getResource(aId + "." + Record.RESOURCEKEY);
    }
    if (resource != null) {
      resource = (Resource) resource.get(Record.RESOURCEKEY);
    }
    return resource;
  }

  public List<Resource> getResources(@Nonnull String aField, @Nonnull Object aValue) {
    List<Resource> records = mElasticsearchRepo.getResources(aField, aValue);
    if (records != null) {
      return unwrapRecords(records);
    }
    return null;
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
    return mElasticsearchRepo.getResource(aId + "." + Record.RESOURCEKEY);
    // if (record == null || record.isEmpty()) {
    // record = mFileRepo.getResource(aId + "." + Record.RESOURCEKEY);
    // }
    // return record;
  }

  private List<Resource> unwrapRecords(List<Resource> aRecords) {
    List<Resource> resources = new ArrayList<Resource>();
    for (Resource rec : aRecords) {
      resources.add((Resource) rec.get(Record.RESOURCEKEY));
    }
    return resources;
  }

  @Override
  public Resource aggregate(@Nonnull AggregationBuilder<?> aAggregationBuilder) throws IOException {
    return aggregate(aAggregationBuilder, null);
  }

  public Resource aggregate(@Nonnull AggregationBuilder<?> aAggregationBuilder, QueryContext aQueryContext)
      throws IOException {
    return mElasticsearchRepo.aggregate(aAggregationBuilder, aQueryContext);
  }

  @Override
  public List<Resource> getAll(@Nonnull String aType) {
    List<Resource> resources = new ArrayList<Resource>();
    try {
      resources = mElasticsearchRepo.getAll(aType);
    } catch (IOException e) {
      Logger.error(e.toString());
    }
    return resources;
  }

}
