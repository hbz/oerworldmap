package services.repository;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
import models.Commit;
import models.GraphHistory;
import models.TripleCommit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.json.simple.parser.ParseException;

import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.typesafe.config.Config;

import models.Resource;
import models.ResourceList;
import play.Logger;
import services.IndexQueue;
import services.QueryContext;
import services.ResourceIndexer;

public class BaseRepository extends Repository
    implements Readable, Writable, Queryable, Aggregatable, Versionable {

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
    Commit.Diff diff = mTriplestoreRepository.getDiff(resource).reverse();
    mElasticsearchRepo.deleteResource(aId);

    mIndexQueue.tell(diff, mIndexQueue);

    return resource;

  }

  @Override
  public void addResource(@Nonnull Resource aResource, Map<String, String> aMetadata) throws IOException {

    if (aMetadata.get(TripleCommit.Header.AUTHOR_HEADER) == null) {
      aMetadata.put(TripleCommit.Header.AUTHOR_HEADER, "Anonymous");
    }
    if (aMetadata.get(TripleCommit.Header.DATE_HEADER) == null) {
      aMetadata.put(TripleCommit.Header.DATE_HEADER, ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

    TripleCommit.Header header = new TripleCommit.Header(aMetadata.get(TripleCommit.Header.AUTHOR_HEADER),
      ZonedDateTime.parse(aMetadata.get(TripleCommit.Header.DATE_HEADER)));

    Commit.Diff diff = mTriplestoreRepository.getDiff(aResource);
    Commit commit = new TripleCommit(header, diff);

    mTriplestoreRepository.commit(commit);
    mIndexQueue.tell(commit, mIndexQueue);

  }

  /**
   * Add several resources, using a single commit.
   * @param aResources  The resources to add
   * @param aMetadata   The commit metadata
   * @throws IOException
   */
  @Override
  public void addResources(@Nonnull List<Resource> aResources, Map<String, String> aMetadata) throws IOException {

    if (aMetadata.get(TripleCommit.Header.AUTHOR_HEADER) == null) {
      aMetadata.put(TripleCommit.Header.AUTHOR_HEADER, "Anonymous");
    }
    if (aMetadata.get(TripleCommit.Header.DATE_HEADER) == null) {
      aMetadata.put(TripleCommit.Header.DATE_HEADER, ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

    TripleCommit.Header header = new TripleCommit.Header(aMetadata.get(TripleCommit.Header.AUTHOR_HEADER),
      ZonedDateTime.parse(aMetadata.get(TripleCommit.Header.DATE_HEADER)));

    Commit.Diff diff = mTriplestoreRepository.getDiff(aResources);
    Commit commit = new TripleCommit(header, diff);

    mTriplestoreRepository.commit(commit);
    mIndexQueue.tell(commit, mIndexQueue);

  }

  /**
   * As opposed to {@link #addResources}, this method imports resources using individual commits with metadata
   * extracted from a document surrounding the actual resource.
   * @param aResources  The resources to import
   * @throws IOException
   */
  public void importResources(@Nonnull List<Resource> aResources) throws IOException {

    Map<String, String> aMetadata = new HashMap<>();

    if (aMetadata.get(TripleCommit.Header.AUTHOR_HEADER) == null) {
      aMetadata.put(TripleCommit.Header.AUTHOR_HEADER, "Anonymous");
    }
    if (aMetadata.get(TripleCommit.Header.DATE_HEADER) == null) {
      aMetadata.put(TripleCommit.Header.DATE_HEADER, ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

    TripleCommit.Header header = new TripleCommit.Header(aMetadata.get(TripleCommit.Header.AUTHOR_HEADER),
      ZonedDateTime.parse(aMetadata.get(TripleCommit.Header.DATE_HEADER)));

    List<Commit> commits = new ArrayList<>();
    for (Resource resource : aResources) {
      Commit.Diff diff = mTriplestoreRepository.getDiff(resource);
      Commit commit = new TripleCommit(header, diff);
      commits.add(commit);
    }
    mTriplestoreRepository.commit(commits);
    for (Commit commit : commits) {
      mIndexQueue.tell(commit, mIndexQueue);
    }

  }

  public ProcessingReport validateAndAdd(Resource aResource, Map<String, String> aMetadata) throws IOException {

    Resource staged = mTriplestoreRepository.stage(aResource);
    ProcessingReport processingReport = staged.validate();
    if (processingReport.isSuccess()) {
      addResource(aResource, aMetadata);
    } else {
      Logger.debug(staged.toString());
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
    return resourceList;
  }

  @Override
  public Resource getResource(@Nonnull String aId) {
    return mTriplestoreRepository.getResource(aId);
  }

  public List<Resource> getResources(@Nonnull String aField, @Nonnull Object aValue) {
    return mElasticsearchRepo.getResources(aField, aValue);
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

  @Override
  public Resource stage(Resource aResource) throws IOException {
    return mTriplestoreRepository.stage(aResource);
  }

  @Override
  public List<Commit> log(String aId) {
    return mTriplestoreRepository.log(aId);
  }

  @Override
  public void commit(Commit aCommit) throws IOException {
    mTriplestoreRepository.commit(aCommit);
  }

  @Override
  public Commit.Diff getDiff(Resource aResource) {
    return mTriplestoreRepository.getDiff(aResource);
  }

  @Override
  public Commit.Diff getDiff(List<Resource> aResources) {
    return mTriplestoreRepository.getDiff(aResources);
  }

}
