package services.repository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.util.List;

import javax.annotation.Nonnull;

import com.hp.hpl.jena.rdf.model.Statement;
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
    "  <%1$s> ?p ?o FILTER isIRI(?o) OPTIONAL{?o ?pp ?oo FILTER isIRI(?oo)}" +
    "}";


  public static final String DESCRIBE_DBSTATE = "DESCRIBE <%s>";

  private final Model db;
  private final GraphHistory mGraphHistory;
  private final Model inverseRelations;

  public TriplestoreRepository(Config aConfiguration) throws IOException {
    this(aConfiguration, ModelFactory.createDefaultModel());
  }

  public TriplestoreRepository(Config aConfiguration, Model aModel) throws IOException {
    this(aConfiguration, aModel, new GraphHistory(Files.createTempDirectory(null).toFile(),
      Files.createTempFile(null, null).toFile()));
  }

  public TriplestoreRepository(Config aConfiguration, Model aModel, GraphHistory aGraphHistory) {
    super(aConfiguration);
    this.db = aModel;
    this.mGraphHistory = aGraphHistory;
    this.inverseRelations = ModelFactory.createDefaultModel();
    try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("inverses.ttl")) {
      RDFDataMgr.read(inverseRelations, inputStream, Lang.TURTLE);
    } catch (IOException e) {
      throw new RuntimeIOException(e);
    }
  }

  @Override
  public Resource getResource(@Nonnull String aId) {

    // Current data
    String describeStatement = String.format(DESCRIBE_RESOURCE, aId);
    Model dbstate = ModelFactory.createDefaultModel();
    QueryExecutionFactory.create(QueryFactory.create(describeStatement), db).execDescribe(dbstate);

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
    return null;
  }

  @Override
  public void addResource(@Nonnull Resource aResource, @Nonnull String aType) throws IOException {

    TripleCommit.Diff diff = getDiff(aResource);
    diff.apply(db);
    // FIXME: set proper commit author
    TripleCommit commit = new TripleCommit(new TripleCommit.Header("Anonymous", ZonedDateTime.now()), diff);
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
    QueryExecutionFactory.create(QueryFactory.create(describeStatement), db).execDescribe(dbstate);

    // Inverses in dbstate, or rather select them from DB?
    addInverses(dbstate);

    Logger.debug(dbstate.toString());
    Logger.debug(model.toString());

    // Create diff
    TripleCommit.Diff diff = new TripleCommit.Diff();

    // Add statements that are in model but not in db
    StmtIterator itAdd = model.difference(dbstate).listStatements();
    while (itAdd.hasNext()) {
      diff.addStatement(itAdd.next());
    }

    // Remove statements that are in db but not in model
    StmtIterator itRemove = dbstate.difference(model).listStatements();
    while (itRemove.hasNext()) {
      diff.removeStatement(itRemove.next());
    }

    Logger.debug(diff.toString());

    return diff;

  }

  @Override
  public Resource deleteResource(@Nonnull String aId) throws IOException {
    return null;
  }

  private void addInverses(Model model) {
    Model inverses = ModelFactory.createDefaultModel();
    for (Statement stmt : inverseRelations.listStatements().toList()) {
      String inferConstruct = String.format(CONSTRUCT_INVERSE, stmt.getSubject(), stmt.getObject());
      QueryExecutionFactory.create(QueryFactory.create(inferConstruct), model).execConstruct(inverses);
    }
    model.add(inverses);
  }

}
