package services.repository;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import models.Commit;
import models.GraphHistory;
import models.Resource;
import models.ResourceList;
import models.TripleCommit;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.tdb.TDBFactory;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import play.Logger;
import services.IndexQueue;
import services.QueryContext;
import services.ResourceIndexer;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BaseRepository extends Repository
    implements Readable, Writable, Queryable, Aggregatable, Versionable {

  private ElasticsearchRepository mESWebpageRepo;
  // TODO: add mESActionRepository here & implement in parallel to mESWebpageRepo
  // TODO: Alternatively, implement a second BaseRepository - but that should be disadvantageous in points of coordinating Actions with WebPages.
  private TriplestoreRepository mTriplestoreWebpageRepo;
  private ResourceIndexer mResourceIndexer;
  // TODO: @ fo: presumably, we'll need a second ResourceIndexer as well?
  // TODO: Or should we favour making the single one configurable?
  private ActorRef mIndexQueue;
  private boolean mAsyncIndexing;

  public BaseRepository(final Config aConfiguration, final ElasticsearchRepository aESWebpageRepo) throws IOException {

    super(aConfiguration);

    if (aESWebpageRepo == null) {
      mESWebpageRepo = new ElasticsearchRepository(mConfiguration);
    }
    else {
      mESWebpageRepo = aESWebpageRepo;
    }
    Dataset dataset;

    try {
      dataset = TDBFactory.createDataset(mConfiguration.getString("tdb.dir"));
    } catch (ConfigException e) {
      Logger.warn("No persistent TDB configured", e);
      dataset = DatasetFactory.create();
    }

    File commitDir = new File(mConfiguration.getString("graph.history.dir"));
    if (!commitDir.exists()) {
      Logger.warn("Commit dir does not exist");
      if (!commitDir.mkdir()) {
        throw new IOException("Could not create commit dir");
      }
    }

    File historyFile = new File(mConfiguration.getString("graph.history.file"));
    if (!historyFile.exists()) {
      Logger.warn("History file does not exist");
      if (!historyFile.createNewFile()) {
        throw new IOException("Could not create history file");
      }
    }
    GraphHistory graphHistory = new GraphHistory(commitDir, historyFile);

    Model mDb = dataset.getDefaultModel();
    mResourceIndexer = new ResourceIndexer(mDb, mESWebpageRepo, graphHistory);

    if (mDb.isEmpty() && mConfiguration.getBoolean("graph.history.autoload")) {
      List<Commit> commits = graphHistory.log();
      Collections.reverse(commits);
      for (Commit commit: commits) {
        commit.getDiff().apply(mDb);
      }
      Logger.info("Loaded commit history to triple store");
      mResourceIndexer.index("*");
      Logger.info("Indexed all resources from triple store");
    }

    mIndexQueue = ActorSystem.create().actorOf(IndexQueue.props(mResourceIndexer));
    mTriplestoreWebpageRepo = new TriplestoreRepository(mConfiguration, mDb, graphHistory);

    mAsyncIndexing = mConfiguration.getBoolean("index.async");

  }

  @Override
  public Resource deleteResource(@Nonnull String aId, Map<String, String> aMetadata) throws IOException {

    Resource resource = mTriplestoreWebpageRepo.deleteResource(aId, aMetadata);

    if (resource != null) {
      mESWebpageRepo.deleteResource(aId, aMetadata);
      Commit.Diff diff = mTriplestoreWebpageRepo.getDiff(resource).reverse();
      index(diff);
    }

    return resource;

  }

  @Override
  public void addResource(@Nonnull Resource aResource, Map<String, String> aMetadata) throws IOException {

    TripleCommit.Header header = new TripleCommit.Header(aMetadata.get(TripleCommit.Header.AUTHOR_HEADER),
      ZonedDateTime.parse(aMetadata.get(TripleCommit.Header.DATE_HEADER)));

    Commit.Diff diff = mTriplestoreWebpageRepo.getDiff(aResource);
    Commit commit = new TripleCommit(header, diff);

    mTriplestoreWebpageRepo.commit(commit);
    index(diff);

  }

  /**
   * Add several CBDs of resources, using individual commits with metadata provided in Map
   * @param aResources
   *          The resources to be added
   * @param aMetadata
   *          The metadata to use
   * @throws IOException
   */
  @Override
  public void addResources(@Nonnull List<Resource> aResources, Map<String, String> aMetadata) throws IOException {

    TripleCommit.Header header = new TripleCommit.Header(aMetadata.get(TripleCommit.Header.AUTHOR_HEADER),
      ZonedDateTime.parse(aMetadata.get(TripleCommit.Header.DATE_HEADER)));

    List<Commit> commits = new ArrayList<>();
    Commit.Diff indexDiff = new TripleCommit.Diff();
    for (Resource resource : aResources) {
      Commit.Diff diff = mTriplestoreWebpageRepo.getDiff(resource);
      indexDiff.append(diff);
      commits.add(new TripleCommit(header, diff));
    }

    mTriplestoreWebpageRepo.commit(commits);
    index(indexDiff);

  }

  /**
   * Import resources, extracting any embedded resources and adding those too, in the same commit
   * @param aResources
   *          The resources to flatten and import
   * @throws IOException
   */
  public void importResources(@Nonnull List<Resource> aResources, Map<String, String> aMetadata) throws IOException {

    Commit.Diff diff = mTriplestoreWebpageRepo.getDiffs(aResources);

    TripleCommit.Header header = new TripleCommit.Header(aMetadata.get(TripleCommit.Header.AUTHOR_HEADER),
      ZonedDateTime.parse(aMetadata.get(TripleCommit.Header.DATE_HEADER)));
    mTriplestoreWebpageRepo.commit(new TripleCommit(header, diff));
    index(diff);

  }

  @Override
  public ResourceList query(@Nonnull String aQueryString, int aFrom, int aSize, String aSortOrder,
                            Map<String, List<String>> aFilters) {
    return query(aQueryString, aFrom, aSize, aSortOrder, aFilters, null);
  }

  public ResourceList query(@Nonnull String aQueryString, int aFrom, int aSize, String aSortOrder,
                            Map<String, List<String>> aFilters, QueryContext aQueryContext) {
    ResourceList resourceList;
    try {
      resourceList = mESWebpageRepo.query(aQueryString, aFrom, aSize, aSortOrder, aFilters, aQueryContext);
    } catch (IOException e) {
      Logger.error("Could not query Elasticsearch repository", e);
      return null;
    }
    return resourceList;
  }

  @Override
  public Resource getResource(@Nonnull String aId) {
    return getResource(aId, null);
  }

  @Override
  public Resource getResource(@Nonnull String aId, String aVersion) {
    return mTriplestoreWebpageRepo.getResource(aId, aVersion);
  }

  public List<Resource> getResources(@Nonnull String aField, @Nonnull Object aValue) {
    return mESWebpageRepo.getResources(aField, aValue);
  }

  @Override
  public Resource aggregate(@Nonnull AggregationBuilder<?> aAggregationBuilder) throws IOException {
    return aggregate(aAggregationBuilder, null);
  }

  public Resource aggregate(@Nonnull AggregationBuilder<?> aAggregationBuilder, QueryContext aQueryContext)
      throws IOException {
    return mESWebpageRepo.aggregate(aAggregationBuilder, aQueryContext);
  }

  public Resource aggregate(@Nonnull List<AggregationBuilder<?>> aAggregationBuilders, QueryContext aQueryContext)
      throws IOException {
    return mESWebpageRepo.aggregate(aAggregationBuilders, aQueryContext);
  }

  @Override
  public List<Resource> getAll(@Nonnull String aType) {
    List<Resource> resources = new ArrayList<>();
    try {
      resources = mESWebpageRepo.getAll(aType);
    } catch (IOException e) {
      Logger.error("Could not query Elasticsearch repository", e);
    }
    return resources;
  }

  @Override
  public Resource stage(Resource aResource) throws IOException {
    return mTriplestoreWebpageRepo.stage(aResource);
  }

  @Override
  public List<Commit> log(String aId) {
    return mTriplestoreWebpageRepo.log(aId);
  }

  @Override
  public void commit(Commit aCommit) throws IOException {
    mTriplestoreWebpageRepo.commit(aCommit);
  }

  @Override
  public Commit.Diff getDiff(Resource aResource) {
    return mTriplestoreWebpageRepo.getDiff(aResource);
  }

  @Override
  public Commit.Diff getDiff(List<Resource> aResources) {
    return mTriplestoreWebpageRepo.getDiff(aResources);
  }

  public void index(String aId) {

    if (mAsyncIndexing) {
      mIndexQueue.tell(aId, mIndexQueue);
    } else {
      mResourceIndexer.index(aId);
    }

  }

  public String sparql(String q) {

    return mTriplestoreWebpageRepo.sparql(q);

  }

  public String update(String delete, String insert, String where) {

    Commit.Diff diff = mTriplestoreWebpageRepo.update(delete, insert, where);
    return diff.toString();

  }

  public String label(String aId) {

    return mTriplestoreWebpageRepo.label(aId);

  }

  private void index(Commit.Diff aDiff) {

    if (mAsyncIndexing) {
      mIndexQueue.tell(aDiff, mIndexQueue);
    } else {
      mResourceIndexer.index(aDiff);
    }

  }

}
