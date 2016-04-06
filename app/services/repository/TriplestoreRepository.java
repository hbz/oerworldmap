package services.repository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.tdb.TDB;
import models.GraphHistory;
import models.TripleCommit;
import org.apache.jena.atlas.RuntimeIOException;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.typesafe.config.Config;

import models.Resource;
import play.Logger;
import services.ResourceFramer;

/**
 * Created by fo on 10.12.15.
 */
public class TriplestoreRepository extends Repository implements Readable, Writable {

  public static final String CONSTRUCT_INVERSE =
    "CONSTRUCT {?o <%1$s> ?s} WHERE {" +
    "  ?s <%2$s> ?o ." +
    "}";

  public static final String DESCRIBE_RESOURCE =
    "DESCRIBE <%1$s> ?o ?oo WHERE {" +
    "  <%1$s> ?p ?o FILTER isIRI(?o) OPTIONAL { ?o ?pp ?oo FILTER isIRI(?oo) }" +
    "}";

  public static final String DESCRIBE_DBSTATE = "DESCRIBE <%s>";

  public static final String SELECT_RESOURCES = "SELECT ?s WHERE { ?s a <%1$s> }";

  private final Model mDb;
  private final GraphHistory mGraphHistory;
  private final Model mInverseRelations;

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
    this.mInverseRelations = ModelFactory.createDefaultModel();
    try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("inverses.ttl")) {
      RDFDataMgr.read(mInverseRelations, inputStream, Lang.TURTLE);
    } catch (IOException e) {
      throw new RuntimeIOException(e);
    }
  }

  @Override
  public Resource getResource(@Nonnull String aId) {

    // Current data
    String describeStatement = String.format(DESCRIBE_RESOURCE, aId);
    Model dbstate = ModelFactory.createDefaultModel();

    dbstate.enterCriticalSection(Lock.WRITE);
    mDb.enterCriticalSection(Lock.READ);

    try {
      try (QueryExecution queryExecution = QueryExecutionFactory.create(QueryFactory.create(describeStatement), mDb)) {
        queryExecution.execDescribe(dbstate);
      }
    } finally {
      mDb.leaveCriticalSection();
      dbstate.leaveCriticalSection();
    }

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

    if (aMetadata.get(TripleCommit.Header.AUTHOR_HEADER) == null) {
      aMetadata.put(TripleCommit.Header.AUTHOR_HEADER, "foo");
    }
    if (aMetadata.get(TripleCommit.Header.DATE_HEADER) == null) {
      aMetadata.put(TripleCommit.Header.DATE_HEADER, ZonedDateTime.now().toString());
    }

    TripleCommit.Header header = new TripleCommit.Header(aMetadata.get(TripleCommit.Header.AUTHOR_HEADER),
      ZonedDateTime.parse(aMetadata.get(TripleCommit.Header.DATE_HEADER)));
    TripleCommit.Diff diff = getDiff(aResource);

    mDb.enterCriticalSection(Lock.WRITE);
    try {
      diff.apply(mDb);
      TDB.sync(mDb);
    } finally {
      mDb.leaveCriticalSection();
    }

    TripleCommit commit = new TripleCommit(header, diff);
    mGraphHistory.add(commit);

  }

  public TripleCommit.Diff getDiff(@Nonnull Resource aResource) {

    // The incoming model
    Model model = ModelFactory.createDefaultModel();

    try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(aResource.toString().getBytes())) {
      RDFDataMgr.read(model, byteArrayInputStream, Lang.JSONLD);
    } catch (IOException e) {
      throw new RuntimeIOException(e);
    }

    // Add inferred (e.g. inverse) statements to incoming model
    addInverses(model);

    // Current data
    String describeStatement = String.format(DESCRIBE_DBSTATE, aResource.getId());
    Model dbstate = ModelFactory.createDefaultModel();

    dbstate.enterCriticalSection(Lock.WRITE);
    mDb.enterCriticalSection(Lock.READ);
    try {
      try (QueryExecution queryExecution = QueryExecutionFactory.create(QueryFactory.create(describeStatement), mDb)) {
        queryExecution.execDescribe(dbstate);
      }
    } finally {
      mDb.leaveCriticalSection();
      dbstate.leaveCriticalSection();
    }


    // Inverses in dbstate, or rather select them from DB?
    addInverses(dbstate);

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

  @Override
  public Resource deleteResource(@Nonnull String aId) throws IOException {

    // Current data
    String describeStatement = String.format(DESCRIBE_DBSTATE, aId);
    Model dbstate = ModelFactory.createDefaultModel();

    dbstate.enterCriticalSection(Lock.WRITE);
    mDb.enterCriticalSection(Lock.READ);
    try {
      try (QueryExecution queryExecution = QueryExecutionFactory.create(QueryFactory.create(describeStatement), mDb)) {
        queryExecution.execDescribe(dbstate);
      }
    } finally {
      mDb.leaveCriticalSection();
      dbstate.leaveCriticalSection();
    }

    // Inverses in dbstate, or rather select them from DB?
    addInverses(dbstate);

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
    // FIXME: set proper commit author
    TripleCommit commit = new TripleCommit(new TripleCommit.Header("Anonymous", ZonedDateTime.now()), diff);
    mGraphHistory.add(commit);

    return ResourceFramer.resourceFromModel(dbstate, aId);

  }

  private void addInverses(Model model) {

    // TODO: this could well be an enricher, such as the broader concept enricher
    Model inverses = ModelFactory.createDefaultModel();

    mInverseRelations.enterCriticalSection(Lock.READ);
    model.enterCriticalSection(Lock.READ);
    try {
      for (Statement stmt : mInverseRelations.listStatements().toList()) {
        String inferConstruct = String.format(CONSTRUCT_INVERSE, stmt.getSubject(), stmt.getObject());
        QueryExecutionFactory.create(QueryFactory.create(inferConstruct), model).execConstruct(inverses);
      }
    } finally {
      model.leaveCriticalSection();
      mInverseRelations.leaveCriticalSection();
    }

    model.enterCriticalSection(Lock.WRITE);
    try {
      model.add(inverses);
    } finally {
      model.leaveCriticalSection();
    }

  }

}
