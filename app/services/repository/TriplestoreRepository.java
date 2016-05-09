package services.repository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.jena.atlas.RuntimeIOException;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.tdb.TDB;
import com.typesafe.config.Config;

import models.Commit;
import models.GraphHistory;
import models.Resource;
import models.TripleCommit;
import play.Logger;
import services.BroaderConceptEnricher;
import services.InverseEnricher;
import services.ResourceEnricher;
import services.ResourceFramer;

/**
 * Created by fo on 10.12.15.
 */
public class TriplestoreRepository extends Repository implements Readable, Writable, Versionable {

  public static final String EXTENDED_DESCRIPTION =
    "DESCRIBE <%1$s> ?o ?oo WHERE {" +
    "  <%1$s> ?p ?o FILTER isIRI(?o) OPTIONAL { ?o ?pp ?oo FILTER isIRI(?oo) }" +
    "}";

  public static final String CONCISE_BOUNDED_DESCRIPTION = "DESCRIBE <%s>";

  public static final String CONCISE_BOUNDED_DESCRIPTIONS = "DESCRIBE ?s WHERE { ?s a ?o . FILTER (%1$s) }";

  public static final String SELECT_LINKS = "SELECT ?o WHERE { <%1$s> ?p ?o FILTER isIRI(?o) }";

  public static final String CONSTRUCT_BACKLINKS = "CONSTRUCT { ?s ?p <%1$s> } WHERE { ?s ?p <%1$s> }";

  public static final String SELECT_RESOURCES = "SELECT ?s WHERE { ?s a <%1$s> }";

  private final Model mDb;
  private final GraphHistory mGraphHistory;
  private final InverseEnricher mInverseEnricher = new InverseEnricher();
  private final ResourceEnricher mBroaderConceptEnricher = new BroaderConceptEnricher();

  public TriplestoreRepository(Config aConfiguration) throws IOException {
    this(aConfiguration, ModelFactory.createDefaultModel());
  }

  public TriplestoreRepository(Config aConfiguration, Model aModel) throws IOException {
    this(aConfiguration, aModel, new GraphHistory(Files.createTempDirectory(null).toFile(),
      Files.createTempFile(null, null).toFile()));
  }

  public TriplestoreRepository(Config aConfiguration, Model aModel, GraphHistory aGraphHistory) {
    super(aConfiguration);
    this.mDb = aModel;
    this.mGraphHistory = aGraphHistory;
  }

  @Override
  public Resource getResource(@Nonnull String aId) {

    Model dbstate = getExtendedDescription(aId, mDb);

    Resource resource = null;
    if (!dbstate.isEmpty()) {
      try {
        resource = ResourceFramer.resourceFromModel(dbstate, aId);
      } catch (IOException e) {
        Logger.error(e.toString());
      }
    }

    return resource;

  }

  @Override
  public List<Resource> getAll(@Nonnull String aType) throws IOException {

    List<Resource> resources = new ArrayList<>();

    mDb.enterCriticalSection(Lock.READ);
    try {
      try (QueryExecution queryExecution = QueryExecutionFactory.create(QueryFactory.create(String.format(SELECT_RESOURCES, aType)), mDb)) {
        ResultSet resultSet = queryExecution.execSelect();
        while (resultSet.hasNext()) {
          QuerySolution querySolution = resultSet.next();
          resources.add(getResource(querySolution.get("s").toString()));
        }
      }
    } finally {
      mDb.leaveCriticalSection();
    }

    return resources;

  }

  @Override
  public void addResource(@Nonnull Resource aResource, Map<String, String> aMetadata) throws IOException {

    TripleCommit.Header header = new TripleCommit.Header(aMetadata.get(TripleCommit.Header.AUTHOR_HEADER),
      ZonedDateTime.parse(aMetadata.get(TripleCommit.Header.DATE_HEADER)));
    Commit.Diff diff = getDiff(aResource);

    commit(new TripleCommit(header, diff));

  }

  @Override
  public void addResources(@Nonnull List<Resource> aResources, Map<String, String> aMetadata) throws IOException {

    TripleCommit.Header header = new TripleCommit.Header(aMetadata.get(TripleCommit.Header.AUTHOR_HEADER),
      ZonedDateTime.parse(aMetadata.get(TripleCommit.Header.DATE_HEADER)));
    Commit.Diff diff = getDiff(aResources);

    commit(new TripleCommit(header, diff));

  }

  @Override
  public void commit(Commit commit) throws IOException {

    mDb.enterCriticalSection(Lock.WRITE);
    try {
      commit.getDiff().apply(mDb);
      TDB.sync(mDb);
    } finally {
      mDb.leaveCriticalSection();
    }

    mGraphHistory.add(commit);

  }

  public void commit(List<Commit> commits) throws IOException {

    mDb.enterCriticalSection(Lock.WRITE);
    try {
      for (Commit commit : commits) {
        commit.getDiff().apply(mDb);
        mGraphHistory.add(commit);
      }
      TDB.sync(mDb);
    } finally {
      mDb.leaveCriticalSection();
    }

  }

  @Override
  public Resource stage(Resource aResource) throws IOException {

    // Get and update current state from database
    Commit.Diff diff = getDiff(aResource);
    Model staged = getExtendedDescription(aResource.getId(), mDb);
    diff.apply(staged);

    // The incoming model
    Model incoming = ModelFactory.createDefaultModel();
    try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(aResource.toString().getBytes())) {
      RDFDataMgr.read(incoming, byteArrayInputStream, Lang.JSONLD);
    } catch (IOException e) {
      throw new RuntimeIOException(e);
    }

    // Select resources incoming model is referencing and add them to staged
    try (QueryExecution queryExecution = QueryExecutionFactory.create(
        QueryFactory.create(String.format(SELECT_LINKS, aResource.getId())), incoming)) {
      ResultSet resultSet = queryExecution.execSelect();
      List<String> links = new ArrayList<>();
      while (resultSet.hasNext()) {
        QuerySolution querySolution = resultSet.next();
        links.add(querySolution.get("o").toString());
      }
      // Only add statements that don't have the original resource as their subject.
      // staged.add(getConciseBoundedDescriptions(links)) would be simpler, but mistakenly
      // duplicates bnodes under certain circumstances.
      // See {@link services.TriplestoreRepositoryTest#testStageWithBnodeInSelfReference}
      StmtIterator it = getConciseBoundedDescriptions(links, mDb).listStatements();
      while (it.hasNext()) {
        Statement statement = it.next();
        if (!statement.getSubject().toString().equals(aResource.getId())) {
          staged.add(statement);
        }
      }
    }

    return ResourceFramer.resourceFromModel(staged, aResource.getId());

  }

  private Model getExtendedDescription(@Nonnull String aId, @Nonnull Model aModel) {

    // Current data
    String describeStatement = String.format(EXTENDED_DESCRIPTION, aId);
    Model extendedDescription = ModelFactory.createDefaultModel();

    extendedDescription.enterCriticalSection(Lock.WRITE);
    aModel.enterCriticalSection(Lock.READ);

    try {
      try (QueryExecution queryExecution = QueryExecutionFactory.create(QueryFactory.create(describeStatement), aModel)) {
        queryExecution.execDescribe(extendedDescription);
      }
    } finally {
      aModel.leaveCriticalSection();
      extendedDescription.leaveCriticalSection();
    }

    return extendedDescription;

  }

  private Model getConciseBoundedDescriptions(@Nonnull List<String> aIds, @Nonnull Model aModel) {

    String filter = String.join(" || ", aIds.stream().map(id -> "?s = <".concat(id).concat(">"))
        .collect(Collectors.toSet()));

    String describeStatement = String.format(CONCISE_BOUNDED_DESCRIPTIONS, filter);
    Model conciseBoundedDescriptions = ModelFactory.createDefaultModel();

    conciseBoundedDescriptions.enterCriticalSection(Lock.WRITE);
    aModel.enterCriticalSection(Lock.READ);

    try {
      try (QueryExecution queryExecution = QueryExecutionFactory.create(QueryFactory.create(describeStatement), aModel)) {
        queryExecution.execDescribe(conciseBoundedDescriptions);
      }
    } finally {
      aModel.leaveCriticalSection();
      conciseBoundedDescriptions.leaveCriticalSection();
    }

    return conciseBoundedDescriptions;

  }

  private Model getConciseBoundedDescription(String aId, Model aModel) {

    String describeStatement = String.format(CONCISE_BOUNDED_DESCRIPTION, aId);
    Model conciseBoundedDescription = ModelFactory.createDefaultModel();

    aModel.enterCriticalSection(Lock.READ);
    try {
      try (QueryExecution queryExecution = QueryExecutionFactory.create(QueryFactory.create(describeStatement), aModel)) {
        queryExecution.execDescribe(conciseBoundedDescription);
      }
    } finally {
      aModel.leaveCriticalSection();
    }

    return conciseBoundedDescription;

  }

  @Override
  public Commit.Diff getDiff(@Nonnull List<Resource> aResources) {

    Commit.Diff diff = new TripleCommit.Diff();

    for (Resource resource : aResources) {
      diff.append(getDiff(resource));
    }

    return diff;

  }

  @Override
  public Commit.Diff getDiff(@Nonnull Resource aResource) {

    aResource = mBroaderConceptEnricher.enrich(aResource, this);

    // The incoming model
    Model incoming = ModelFactory.createDefaultModel();

    try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(aResource.toString().getBytes())) {
      RDFDataMgr.read(incoming, byteArrayInputStream, Lang.JSONLD);
    } catch (IOException e) {
      throw new RuntimeIOException(e);
    }

    // Reduce incoming model to CBD
    Model model = getConciseBoundedDescription(aResource.getId(), incoming);

    // Add inferred (e.g. inverse) statements to incoming model
    mInverseEnricher.enrich(model);

    // Current data
    Model dbstate = getConciseBoundedDescription(aResource.getId(), mDb);

    // Inverses in dbstate, or rather select them from DB?
    mInverseEnricher.enrich(dbstate);

    // Create diff
    TripleCommit.Diff diff = new TripleCommit.Diff();

    // Add statements that are in model but not in mDb
    StmtIterator itAdd = model.difference(dbstate).listStatements();
    while (itAdd.hasNext()) {
      diff.addStatement(itAdd.next());
    }

    // Remove statements that are in mDb but not in model
    StmtIterator itRemove = dbstate.difference(model).listStatements();
    while (itRemove.hasNext()) {
      diff.removeStatement(itRemove.next());
    }

    return diff;

  }

  public Commit.Diff getDiffs(@Nonnull Resource aResource) {

    List<Resource> resources = new ArrayList<>();
    try {
      resources = ResourceFramer.flatten(aResource);
    } catch (IOException e) {
      Logger.error("Failed to flatten resource", e);
    }

    TripleCommit.Diff diff = new TripleCommit.Diff();
    for (Resource resource : resources) {
      diff.append(getDiff(resource));
    }

    return diff;

  }

  public Commit.Diff getDiffs(@Nonnull List<Resource> aResources) {

    Commit.Diff diff = new TripleCommit.Diff();

    for (Resource resource : aResources) {
      diff.append(getDiffs(resource));
    }

    return diff;

  }

  @Override
  public Resource deleteResource(@Nonnull String aId, Map<String, String> aMetadata) throws IOException {

    // Current data, outbound links
    Model dbstate = getConciseBoundedDescription(aId, mDb);

    // Current data, inbound links
    String constructStatement = String.format(CONSTRUCT_BACKLINKS, aId);
    mDb.enterCriticalSection(Lock.READ);
    try {
      try (QueryExecution queryExecution = QueryExecutionFactory.create(QueryFactory.create(constructStatement), mDb)) {
        queryExecution.execConstruct(dbstate);
      }
    } finally {
      mDb.leaveCriticalSection();
    }

    // Inverses in dbstate, or rather select them from DB?
    mInverseEnricher.enrich(dbstate);

    // Create diff
    TripleCommit.Diff diff = new TripleCommit.Diff();

    // Remove all statements that are in mDb
    StmtIterator itRemove = dbstate.listStatements();
    while (itRemove.hasNext()) {
      diff.removeStatement(itRemove.next());
    }

    mDb.enterCriticalSection(Lock.WRITE);
    try {
      diff.apply(mDb);
      TDB.sync(mDb);
    } finally {
      mDb.leaveCriticalSection();
    }

    // Record removal in history
    TripleCommit.Header header = new TripleCommit.Header(aMetadata.get(TripleCommit.Header.AUTHOR_HEADER),
      ZonedDateTime.parse(aMetadata.get(TripleCommit.Header.DATE_HEADER)));
    TripleCommit commit = new TripleCommit(header, diff);
    mGraphHistory.add(commit);

    return ResourceFramer.resourceFromModel(dbstate, aId);

  }

  @Override
  public List<Commit> log(String aId) {

    return mGraphHistory.log(aId);

  }

}
