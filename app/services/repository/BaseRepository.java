package services.repository;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import models.Record;
import models.TripleCommit;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.json.simple.parser.ParseException;

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

    mResourceIndexer = new ResourceIndexer(dataset.getDefaultModel(), mElasticsearchRepo, graphHistory);
    mIndexQueue = ActorSystem.create().actorOf(IndexQueue.props(mResourceIndexer));
    mTriplestoreRepository = new TriplestoreRepository(aConfiguration, dataset.getDefaultModel(), graphHistory);

  }

  @Override
  public Resource deleteResource(@Nonnull String aId, Map<String, String> aMetadata) throws IOException {

    Resource resource = mTriplestoreRepository.deleteResource(aId, aMetadata);

    if (resource != null) {
      mElasticsearchRepo.deleteResource(aId, aMetadata);
      Commit.Diff diff = mTriplestoreRepository.getDiff(resource).reverse();
      mIndexQueue.tell(diff, mIndexQueue);

    }

    return resource;

  }

  /**
   * Add CBD of a resource with metadata provided in Map
   * @param aResource
   *          The resource to be added
   * @param aMetadata
   *          The metadata to use
   * @throws IOException
   */
  @Override
  public void addResource(@Nonnull Resource aResource, Map<String, String> aMetadata) throws IOException {

    TripleCommit.Header header = new TripleCommit.Header(aMetadata.get(TripleCommit.Header.AUTHOR_HEADER),
      ZonedDateTime.parse(aMetadata.get(TripleCommit.Header.DATE_HEADER)));

    Commit.Diff diff = mTriplestoreRepository.getDiff(aResource);
    Commit commit = new TripleCommit(header, diff);

    mTriplestoreRepository.commit(commit);
    mIndexQueue.tell(diff, mIndexQueue);

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
    mIndexQueue.tell(indexDiff, mIndexQueue);

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
      resource.put("@context", "http://schema.org/");
      Commit.Diff diff = mTriplestoreRepository.getDiff(resource);
      Commit commit = new TripleCommit(new TripleCommit.Header(author, date), diff);
      indexDiff.append(diff);
      mTriplestoreRepository.commit(commit);
    }

    mIndexQueue.tell(indexDiff, mIndexQueue);

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
    mIndexQueue.tell(diff, mIndexQueue);

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
