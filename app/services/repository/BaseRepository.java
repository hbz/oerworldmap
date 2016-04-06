package services.repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.typesafe.config.ConfigException;
import models.GraphHistory;
import models.TripleCommit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.json.simple.parser.ParseException;

import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.typesafe.config.Config;

import models.Record;
import models.Resource;
import models.ResourceList;
import play.Logger;
import services.IndexQueue;
import services.QueryContext;
import services.ResourceIndexer;

public class BaseRepository extends Repository
    implements Readable, Writable, Queryable, Aggregatable {

  private ElasticsearchRepository mElasticsearchRepo;
  private TriplestoreRepository mTriplestoreRepository;
  private ResourceIndexer mResourceIndexer;
  private ActorRef mIndexQueue;

  public BaseRepository(Config aConfiguration) throws IOException {

    super(aConfiguration);

    mElasticsearchRepo = new ElasticsearchRepository(aConfiguration);
    Dataset dataset;

    try {
      dataset = TDBFactory.createDataset(aConfiguration.getString("tdb.dir"));
    } catch (ConfigException e) {
      Logger.warn("No persistent TDB configured", e);
      dataset = DatasetFactory.createMem();
    }
    mResourceIndexer = new ResourceIndexer(dataset.getDefaultModel(), mElasticsearchRepo);
    mIndexQueue = ActorSystem.create().actorOf(IndexQueue.props(mResourceIndexer));

    File commitDir = new File(aConfiguration.getString("graph.history.dir"));
    if (!commitDir.exists()) {
      Logger.warn("Commit dir does not exist");
      if (!commitDir.mkdir()) {
        throw new IOException("Could not create commit dir");
      }
    }

    File historyFile = new File(aConfiguration.getString("graph.history.file"));
    if (!historyFile.exists()) {
      Logger.warn("History file does not exist");
      if (!historyFile.createNewFile()) {
        throw new IOException("Could not create history file");
      }
    }
    GraphHistory graphHistory = new GraphHistory(commitDir, historyFile);

    mTriplestoreRepository = new TriplestoreRepository(aConfiguration, dataset.getDefaultModel(), graphHistory);

  }

  @Override
  public Resource deleteResource(@Nonnull String aId) throws IOException {

    Resource resource = mTriplestoreRepository.deleteResource(aId);
    TripleCommit.Diff diff = mTriplestoreRepository.getDiff(resource).reverse();
    mElasticsearchRepo.deleteResource(aId);
    mIndexQueue.tell(diff, mIndexQueue);

    return resource;

  }

  @Override
  public void addResource(@Nonnull Resource aResource, Map<String, String> aMetadata) throws IOException {

    TripleCommit.Diff diff = mTriplestoreRepository.getDiff(aResource);
    mTriplestoreRepository.addResource(aResource, new HashMap<>());
    mIndexQueue.tell(diff, mIndexQueue);

  }

  public ProcessingReport validateAndAdd(Resource aResource) throws IOException {
    addResource(aResource, new HashMap<>());
    return new ListProcessingReport();
    /*
    FIXME: add validation
    Set<Resource> denormalizedResources = mResourceIndexer.getResources(diff);
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
    */
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
    return mTriplestoreRepository.getResource(aId);
  }

  public List<Resource> getResources(@Nonnull String aField, @Nonnull Object aValue) {
    List<Resource> records = mElasticsearchRepo.getResources(aField, aValue);
    if (records != null) {
      return unwrapRecords(records);
    }
    return null;
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
