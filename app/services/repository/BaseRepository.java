package services.repository;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.*;

public class BaseRepository extends Repository
    implements Readable, Writable, Queryable, Aggregatable, Versionable {

  private ElasticsearchRepository mElasticsearchRepo;
  private TriplestoreRepository mTriplestoreRepository;
  private ResourceIndexer mResourceIndexer;
  private ActorRef mIndexQueue;
  private boolean mAsyncIndexing;

  public BaseRepository(final Config aConfiguration, final ElasticsearchRepository aElasticsearchRepo) throws IOException {

    super(aConfiguration);

    if (aElasticsearchRepo == null) {
      mElasticsearchRepo = new ElasticsearchRepository(mConfiguration);
    }
    else {
      mElasticsearchRepo = aElasticsearchRepo;
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
    mResourceIndexer = new ResourceIndexer(mDb, mElasticsearchRepo, graphHistory);

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
    mTriplestoreRepository = new TriplestoreRepository(mConfiguration, mDb, graphHistory);

    mAsyncIndexing = mConfiguration.getBoolean("index.async");

  }

  @Override
  public Resource deleteResource(@Nonnull String aId, Map<String, String> aMetadata) throws IOException {

    Resource resource = mTriplestoreRepository.deleteResource(aId, aMetadata);

    if (resource != null) {
      mElasticsearchRepo.deleteResource(aId, aMetadata);
      Commit.Diff diff = mTriplestoreRepository.getDiff(resource).reverse();
      index(diff);
    }

    return resource;

  }

  @Override
  public void addResource(@Nonnull Resource aResource, Map<String, String> aMetadata) throws IOException {

    TripleCommit.Header header = new TripleCommit.Header(aMetadata.get(TripleCommit.Header.AUTHOR_HEADER),
      ZonedDateTime.parse(aMetadata.get(TripleCommit.Header.DATE_HEADER)));

    Commit.Diff diff = mTriplestoreRepository.getDiff(aResource);
    Commit commit = new TripleCommit(header, diff);

    mTriplestoreRepository.commit(commit);
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
      Commit.Diff diff = mTriplestoreRepository.getDiff(resource);
      indexDiff.append(diff);
      commits.add(new TripleCommit(header, diff));
    }

    mTriplestoreRepository.commit(commits);
    index(indexDiff);

  }

  /**
   * Import resources, extracting any embedded resources and adding those too, in the same commit
   * @param aResources
   *          The resources to flatten and import
   * @throws IOException
   */
  public void importResources(@Nonnull List<Resource> aResources, Map<String, String> aMetadata) throws IOException {

    Commit.Diff diff = mTriplestoreRepository.getDiffs(aResources);

    TripleCommit.Header header = new TripleCommit.Header(aMetadata.get(TripleCommit.Header.AUTHOR_HEADER),
      ZonedDateTime.parse(aMetadata.get(TripleCommit.Header.DATE_HEADER)));
    mTriplestoreRepository.commit(new TripleCommit(header, diff));
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
      resourceList = mElasticsearchRepo.query(aQueryString, aFrom, aSize, aSortOrder, aFilters, aQueryContext);
    } catch (IOException e) {
      Logger.error("Could not query Elasticsearch repository", e);
      return null;
    }
    return resourceList;
  }

  public JsonNode reconcile(@Nonnull String aQueryString, int aFrom, int aSize, String aSortOrder,
                            Map<String, List<String>> aFilters, QueryContext aQueryContext,
                            final Locale aPreferredLocale) {
    return mElasticsearchRepo
      .reconcile(aQueryString, aFrom, aSize, aSortOrder, aFilters, aQueryContext, aPreferredLocale);
  }

  @Override
  public Resource getResource(@Nonnull String aId) {
    return getResource(aId, null);
  }

  @Override
  public Resource getResource(@Nonnull String aId, String aVersion) {
    return mTriplestoreRepository.getResource(aId, aVersion);
  }

  public List<Resource> getResources(@Nonnull String aField, @Nonnull Object aValue) {
    return mElasticsearchRepo.getResources(aField, aValue);
  }

  @Override
  public Resource aggregate(@Nonnull AggregationBuilder aAggregationBuilder) throws IOException {
    return aggregate(aAggregationBuilder, null);
  }

  public Resource aggregate(@Nonnull AggregationBuilder aAggregationBuilder, QueryContext aQueryContext)
      throws IOException {
    return mElasticsearchRepo.aggregate(aAggregationBuilder, aQueryContext);
  }

  public Resource aggregate(@Nonnull List<AggregationBuilder> aAggregationBuilders, QueryContext aQueryContext)
      throws IOException {
    return mElasticsearchRepo.aggregate(aAggregationBuilders, aQueryContext);
  }

  @Override
  public List<Resource> getAll(@Nonnull String aType) {
    List<Resource> resources = new ArrayList<>();
    try {
      resources = mElasticsearchRepo.getAll(aType);
    } catch (IOException e) {
      Logger.error("Could not query Elasticsearch repository", e);
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

  public void index(String aId) {

    if (mAsyncIndexing) {
      mIndexQueue.tell(aId, mIndexQueue);
    } else {
      mResourceIndexer.index(aId);
    }

  }

  public String sparql(String q) {

    return mTriplestoreRepository.sparql(q);

  }

  public String update(String delete, String insert, String where) {

    Commit.Diff diff = mTriplestoreRepository.update(delete, insert, where);
    return diff.toString();

  }

  public String label(String aId) {

    return mTriplestoreRepository.label(aId);

  }

  private void index(Commit.Diff aDiff) {

    if (mAsyncIndexing) {
      mIndexQueue.tell(aDiff, mIndexQueue);
    } else {
      mResourceIndexer.index(aDiff);
    }

  }

}
