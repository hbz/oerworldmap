package services.repository;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import models.*;
import org.apache.commons.lang3.StringUtils;
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

  private ElasticsearchRepository mESRepo;
  private TriplestoreRepository mTriplestoreRepo;
  private ResourceIndexer mResourceIndexer;
  private ActorRef mIndexQueue;
  private boolean mAsyncIndexing;

  public BaseRepository(final Config aConfiguration, final ElasticsearchRepository aESRepo) throws IOException {

    super(aConfiguration);

    if (aESRepo == null) {
      mESRepo = new ElasticsearchRepository(mConfiguration);
    }
    else {
      mESRepo = aESRepo;
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
    mResourceIndexer = new ResourceIndexer(mDb, mESRepo, graphHistory);

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
    mTriplestoreRepo = new TriplestoreRepository(mConfiguration, mDb, graphHistory);

    mAsyncIndexing = mConfiguration.getBoolean("index.async");

  }

  @Override
  public Resource deleteResource(@Nonnull final String aId,
                                 @Nonnull final String aClassType,
                                 final Map<String, String> aMetadata) throws IOException {
    Resource resource = mTriplestoreRepo.deleteResource(aId, aClassType, aMetadata);
    if (resource != null) {
      mESRepo.deleteResource(aId, aClassType, aMetadata);
      Commit.Diff diff = mTriplestoreRepo.getDiff(resource).reverse();
      index(diff);
    }

    return resource;

  }

  @Override
  public void addResource(@Nonnull Resource aResource, Map<String, String> aMetadata) throws IOException {

    TripleCommit.Header header = new TripleCommit.Header(aMetadata.get(TripleCommit.Header.AUTHOR_HEADER),
      ZonedDateTime.parse(aMetadata.get(TripleCommit.Header.DATE_HEADER)));

    Commit.Diff diff = mTriplestoreRepo.getDiff(aResource);
    Commit commit = new TripleCommit(header, diff);

    mTriplestoreRepo.commit(commit);
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
      Commit.Diff diff = mTriplestoreRepo.getDiff(resource);
      indexDiff.append(diff);
      commits.add(new TripleCommit(header, diff));
    }

    mTriplestoreRepo.commit(commits);
    index(indexDiff);

  }

  /**
   * Import resources, extracting any embedded resources and adding those too, in the same commit
   * @param aResources
   *          The resources to flatten and import
   * @throws IOException
   */
  public void importResources(@Nonnull List<Resource> aResources, Map<String, String> aMetadata) throws IOException {

    Commit.Diff diff = mTriplestoreRepo.getDiffs(aResources);

    TripleCommit.Header header = new TripleCommit.Header(aMetadata.get(TripleCommit.Header.AUTHOR_HEADER),
      ZonedDateTime.parse(aMetadata.get(TripleCommit.Header.DATE_HEADER)));
    mTriplestoreRepo.commit(new TripleCommit(header, diff));
    index(diff);

  }

  @Override
  public ResourceList query(@Nonnull String aQueryString, int aFrom, int aSize, String aSortOrder,
                            Map<String, List<String>> aFilters, final String... aIndices) {
    return query(aQueryString, aFrom, aSize, aSortOrder, aFilters, null, checkIndices(aIndices));
  }

  public ResourceList query(@Nonnull String aQueryString, int aFrom, int aSize, String aSortOrder,
                            Map<String, List<String>> aFilters, QueryContext aQueryContext,
                            final String... aIndices) {
    ResourceList resourceList;
    try {
      resourceList = mESRepo.query(aQueryString, aFrom, aSize, aSortOrder, aFilters, aQueryContext,
        checkIndices(aIndices));
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
    return mTriplestoreRepo.getResource(aId, aVersion);
  }

  public List<Resource> getResources(@Nonnull String aField, @Nonnull Object aValue,
                                     final String... aIndices) {
    return mESRepo.getResources(aField, aValue, checkIndices(aIndices));
  }

  @Override
  public Resource aggregate(@Nonnull AggregationBuilder<?> aAggregationBuilder,
                            final String... aIndices) throws IOException {
    return aggregate(aAggregationBuilder, null, checkIndices(aIndices));
  }

  public Resource aggregate(@Nonnull AggregationBuilder<?> aAggregationBuilder,
                            QueryContext aQueryContext,
    final String... aIndices)
      throws IOException {
    return mESRepo.aggregate(aAggregationBuilder, aQueryContext, checkIndices(aIndices));
  }

  public Resource aggregate(@Nonnull List<AggregationBuilder<?>> aAggregationBuilders,
                            QueryContext aQueryContext, final String... aIndices)
      throws IOException {
    return mESRepo.aggregate(aAggregationBuilders, aQueryContext, checkIndices(aIndices));
  }

  @Override
  public List<Resource> getAll(@Nonnull String aType, final String... aIndices) {
    List<Resource> resources = new ArrayList<>();
    try {
      resources = mESRepo.getAll(aType, checkIndices(aIndices));
    } catch (IOException e) {
      Logger.error("Could not query Elasticsearch repository", e);
    }
    return resources;
  }

  @Override
  public Resource stage(Resource aResource) throws IOException {
    return mTriplestoreRepo.stage(aResource);
  }

  @Override
  public List<Commit> log(String aId) {
    return mTriplestoreRepo.log(aId);
  }

  @Override
  public void commit(Commit aCommit) throws IOException {
    mTriplestoreRepo.commit(aCommit);
  }

  @Override
  public Commit.Diff getDiff(Resource aResource) {
    return mTriplestoreRepo.getDiff(aResource);
  }

  @Override
  public Commit.Diff getDiff(List<Resource> aResources) {
    return mTriplestoreRepo.getDiff(aResources);
  }

  public void index(String aId) {

    if (mAsyncIndexing) {
      mIndexQueue.tell(aId, mIndexQueue);
    } else {
      mResourceIndexer.index(aId);
    }

  }

  public String sparql(String q) {

    return mTriplestoreRepo.sparql(q);

  }

  public String update(String delete, String insert, String where) {

    Commit.Diff diff = mTriplestoreRepo.update(delete, insert, where);
    return diff.toString();

  }

  public String label(String aId) {

    return mTriplestoreRepo.label(aId);

  }

  private void index(Commit.Diff aDiff) {

    if (mAsyncIndexing) {
      mIndexQueue.tell(aDiff, mIndexQueue);
    } else {
      mResourceIndexer.index(aDiff);
    }

  }

  private String[] checkIndices(final String... aIndices){
    if (aIndices == null || aIndices.length == 0){
      return new String[]{"*"};
    }
    for (String index : aIndices){
      if (! StringUtils.isEmpty(index)){
        return aIndices;
      }
    }
    return new String[]{"*"};
  }

}
