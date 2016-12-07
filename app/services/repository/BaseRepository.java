package services.repository;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.tdb.TDBFactory;
import org.elasticsearch.search.aggregations.AggregationBuilder;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import models.Commit;
import models.GraphHistory;
import models.Record;
import models.Resource;
import models.ResourceList;
import models.TripleCommit;
import play.Logger;
import services.IndexQueue;
import services.QueryContext;
import services.ResourceFramer;
import services.ResourceIndexer;

public class BaseRepository extends Repository
    implements Readable, Writable, Queryable, Aggregatable, Versionable {

  private ElasticsearchRepository mElasticsearchRepo;
  private TriplestoreRepository mTriplestoreRepository;
  private ResourceIndexer mResourceIndexer;
  private ActorRef mIndexQueue;
  private boolean mAsyncIndexing;

  public BaseRepository(final Config aConfiguration) throws IOException {

    super(aConfiguration);

    mElasticsearchRepo = new ElasticsearchRepository(mConfiguration);
    Dataset dataset;

    try {
      dataset = TDBFactory.createDataset(mConfiguration.getString("tdb.dir"));
    } catch (ConfigException e) {
      Logger.warn("No persistent TDB configured", e);
      dataset = DatasetFactory.createMem();
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

    Integer framerPort = mConfiguration.getInt("node.framer.port");
    ResourceFramer.setPort(framerPort);
    ResourceFramer.start();

    Model mDb = dataset.getDefaultModel();
    mResourceIndexer = new ResourceIndexer(mDb, mElasticsearchRepo, graphHistory);

    if (mDb.isEmpty() && mConfiguration.getBoolean("graph.history.autoload")) {
      List<Commit> commits = graphHistory.log();
      Collections.reverse(commits);
      for (Commit commit: commits) {
        commit.getDiff().apply(mDb);
      }
      Logger.debug("Loaded commit history to triple store");
      mResourceIndexer.index("*");
      Logger.debug("Indexed all resources from triple store");
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
   * As opposed to {@link #addResources}, this method imports resources using individual commits with metadata
   * extracted from a document surrounding the actual resource (a "record").
   *
   * @param aRecords
   *          The resources to import
   * @throws IOException
   */
  public void importRecords(@Nonnull List<Resource> aRecords, Map<String, String> aDefaultMetadata) throws IOException {

    aRecords.sort(((o1, o2) -> ZonedDateTime.parse(o1.getAsString(Record.DATE_CREATED))
      .compareTo(ZonedDateTime.parse(o2.getAsString(Record.DATE_CREATED)))));

    Commit.Diff indexDiff = new TripleCommit.Diff();
    for (Resource record : aRecords) {
      String author = record.getAsString(Record.AUTHOR);
      if (StringUtils.isEmpty(author)) {
        author = aDefaultMetadata.get(TripleCommit.Header.AUTHOR_HEADER);
      }
      ZonedDateTime date = ZonedDateTime.parse(record.getAsString(Record.DATE_CREATED));
      if (date == null) {
        date = ZonedDateTime.parse(aDefaultMetadata.get(TripleCommit.Header.DATE_HEADER));
      }
      Resource resource = record.getAsResource(Record.RESOURCE_KEY);
      resource.put("@context", mConfiguration.getString("jsonld.context"));
      Commit.Diff diff = mTriplestoreRepository.getDiff(resource);
      Commit commit = new TripleCommit(new TripleCommit.Header(author, date), diff);
      indexDiff.append(diff);
      mTriplestoreRepository.commit(commit);
    }

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
      Logger.error(e.toString());
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
    return mTriplestoreRepository.getResource(aId, aVersion);
  }

  public Resource getRecord(@Nonnull String aId) {
    return mElasticsearchRepo.getRecord(aId + "." + Record.RESOURCE_KEY);
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

  public Resource aggregate(@Nonnull List<AggregationBuilder<?>> aAggregationBuilders, QueryContext aQueryContext)
      throws IOException {
    return mElasticsearchRepo.aggregate(aAggregationBuilders, aQueryContext);
  }

  @Override
  public List<Resource> getAll(@Nonnull String aType) {
    List<Resource> resources = new ArrayList<>();
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

  private void index(Commit.Diff aDiff) {

    if (mAsyncIndexing) {
      mIndexQueue.tell(aDiff, mIndexQueue);
    } else {
      mResourceIndexer.index(aDiff);
    }

  }

}
