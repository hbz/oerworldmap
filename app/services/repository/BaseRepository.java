package services.repository;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.typesafe.config.Config;

import helpers.JsonLdConstants;
import helpers.UniversalFunctions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import models.Record;
import models.Resource;

import models.ResourceList;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.json.simple.parser.ParseException;

import play.Logger;
import services.ElasticsearchProvider;
import services.ResourceDenormalizer;
import services.repository.ElasticsearchRepository;
import services.repository.FileRepository;

import javax.annotation.Nonnull;

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
    List<Resource> denormalizedResources = ResourceDenormalizer.denormalize(aResource, this);
    for (Resource dnr : denormalizedResources) {
      if (dnr.hasId()) {
        Resource rec = getRecord(dnr);
        // Extract the type from the resource, otherwise everything will be typed WebPage!
        String type = dnr.getAsString(JsonLdConstants.TYPE);
        addResource(rec, type);
      }
    }
  }

  @Override
  public void addResource(@Nonnull Resource aResource, @Nonnull String aType) throws IOException {
    mElasticsearchRepo.addResource(aResource, aType);
    mFileRepo.addResource(aResource, aType);
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
        // Extract the type from the resource, otherwise everything will be typed WebPage!
        String type = dnr.getAsString(JsonLdConstants.TYPE);
        addResource(rec, type);
      }
    }
    return processingReport;
  }

  @Override
  public ResourceList query(@Nonnull String aQueryString, int aFrom, int aSize, String aSortOrder) {
    // FIXME: hardcoded access restriction to newsletter-only unsers, criteria:
    // has no unencrypted email address
    String filteredQueryString = aQueryString
        .concat(" AND ((about.@type:Article OR about.@type:Organization OR about.@type:Action OR about.@type:Service)")
        .concat(" OR (about.@type:Person AND about.email:*))");
    ResourceList resourceList;
    try {
      resourceList = mElasticsearchRepo.query(filteredQueryString, aFrom, aSize, aSortOrder);
    } catch (IOException | ParseException e) {
      Logger.error(e.toString());
      return null;
    }
    // set this manually so that the filteredQueryString does not become visible
    resourceList.setSearchTerms(aQueryString);
    // members are Records, unwrap to plain Resources
    List<Resource> resources = new ArrayList<>();
    resources.addAll(getResources(resourceList.getItems()));
    resourceList.setItems(resources);
    return resourceList;
  }

  @Override
  public Resource getResource(@Nonnull String aId) {
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
    return resources;
  }

}
