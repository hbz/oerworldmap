package services.repository;

import com.typesafe.config.Config;
import helpers.SCHEMA;
import models.Commit;
import models.GraphHistory;
import models.Resource;
import models.TripleCommit;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.atlas.RuntimeIOException;
import org.apache.jena.atlas.lib.InternalErrorException;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shared.Lock;
import org.apache.jena.tdb.TDB;
import org.apache.jena.vocabulary.RDF;
import play.Logger;
import services.BroaderConceptEnricher;
import services.InverseEnricher;
import services.ResourceEnricher;
import services.ResourceFramer;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by fo on 10.12.15.
 */
public class TriplestoreRepository extends Repository implements Readable, Writable, Versionable {

  private static final String CONCISE_BOUNDED_DESCRIPTION = "DESCRIBE <%s>";

  private static final String CONSTRUCT_BACKLINKS = "CONSTRUCT { ?s ?p <%1$s> } WHERE { ?s ?p <%1$s> }";

  private static final String SELECT_RESOURCES = "SELECT ?s WHERE { ?s a <%1$s> }";

  private static final String LABEL_RESOURCE = "SELECT ?name WHERE { <%1$s> <http://schema.org/name> ?name  FILTER (lang(?name) = 'en') }";

  private final Model mDb;
  private final GraphHistory mGraphHistory;
  private final ResourceEnricher mInverseEnricher = new InverseEnricher();
  private final ResourceEnricher mBroaderConceptEnricher = new BroaderConceptEnricher();

  public TriplestoreRepository(Config aConfiguration) throws IOException {
    this(aConfiguration, ModelFactory.createDefaultModel());
  }

  public TriplestoreRepository(Config aConfiguration, Model aModel) throws IOException {
    this(aConfiguration, aModel, new GraphHistory(Files.createTempDirectory(null).toFile(),
      Files.createTempFile(null, null).toFile()));
  }

  TriplestoreRepository(Config aConfiguration, Model aModel, GraphHistory aGraphHistory) {
    super(aConfiguration);
    this.mDb = aModel;
    this.mGraphHistory = aGraphHistory;
  }

  @Override
  public Resource getResource(@Nonnull String aId, String aVersion) {
    Model dbstate = getExtendedDescription(aId, mDb);
    if ((aVersion != null) && !("HEAD".equals(aVersion))) {
      for (Commit commit : mGraphHistory.until(aVersion)) {
        commit.getDiff().unapply(dbstate);
      }
    }
    Resource resource = null;
    if (!dbstate.isEmpty()) {
      try {
        resource = ResourceFramer.resourceFromModel(dbstate, aId, mConfiguration.getString("jsonld.context"));
      } catch (IOException e) {
        Logger.error("Could not get resource", e);
      }
    }
    return resource;
  }

  @Override
  public Resource getResource(@Nonnull String aId) {
    return getResource(aId, null);
  }

  @Override
  public List<Resource> getAll(@Nonnull String aType) throws IOException {
    List<Resource> resources = new ArrayList<>();
    mDb.enterCriticalSection(Lock.READ);
    try {
      try (QueryExecution queryExecution = QueryExecutionFactory
        .create(QueryFactory.create(String.format(SELECT_RESOURCES, aType)), mDb)) {
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
  public void addResource(@Nonnull Resource aResource, Map<String, String> aMetadata)
    throws IOException {
    TripleCommit.Header header = new TripleCommit.Header(
      aMetadata.get(TripleCommit.Header.AUTHOR_HEADER),
      ZonedDateTime.parse(aMetadata.get(TripleCommit.Header.DATE_HEADER)));
    Commit.Diff diff = getDiff(aResource);
    commit(new TripleCommit(header, diff));
  }

  @Override
  public void addResources(@Nonnull List<Resource> aResources, Map<String, String> aMetadata)
    throws IOException {

    TripleCommit.Header header = new TripleCommit.Header(
      aMetadata.get(TripleCommit.Header.AUTHOR_HEADER),
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

  // Get and update current state from database
  @Override
  public Resource stage(Resource aResource) throws IOException {
    Commit.Diff diff = getDiff(aResource);
    Model staged = getConciseBoundedDescription(aResource.getId(), mDb);

    diff.apply(staged);
    staged.add(getIdentifyingDescriptions(staged.listObjects(), mDb,
      Collections.singletonList(ResourceFactory.createResource(aResource.getId()))));

    return ResourceFramer.resourceFromModel(staged, aResource.getId(), mConfiguration.getString("jsonld.context"));
  }

  public static Model getExtendedDescription(@Nonnull String aId, @Nonnull Model aModel) {
    Model extendedDescription = getConciseBoundedDescription(aId, aModel);
    extendedDescription.add(getIdentifyingDescriptions(extendedDescription.listObjects(), aModel, new ArrayList<>()));
    return extendedDescription;
  }

  private static Model getIdentifyingDescriptions(NodeIterator subjects, Model aModel, List<RDFNode> skip) {
    Model identifyingDescriptions = ModelFactory.createDefaultModel();
    while (subjects.hasNext()) {
      RDFNode node = subjects.nextNode();
      if (skip.contains(node)) {
        continue;
      }
      if (node.isURIResource()) {
        identifyingDescriptions.add(
          aModel.listStatements((org.apache.jena.rdf.model.Resource) node, RDF.type, (RDFNode) null)
        );
        identifyingDescriptions.add(
          aModel.listStatements((org.apache.jena.rdf.model.Resource) node, SCHEMA.name, (RDFNode) null)
        );
        identifyingDescriptions.add(
          aModel.listStatements((org.apache.jena.rdf.model.Resource) node, SCHEMA.image, (RDFNode) null)
        );
        identifyingDescriptions.add(
          aModel.listStatements((org.apache.jena.rdf.model.Resource) node, SCHEMA.agent, (RDFNode) null)
        );
        identifyingDescriptions.add(
          aModel.listStatements((org.apache.jena.rdf.model.Resource) node, SCHEMA.description, (RDFNode) null)
        );
        identifyingDescriptions.add(
          aModel.listStatements((org.apache.jena.rdf.model.Resource) node, SCHEMA.text, (RDFNode) null)
        );
        identifyingDescriptions.add(
          aModel.listStatements((org.apache.jena.rdf.model.Resource) node, SCHEMA.startTime, (RDFNode) null)
        );
        identifyingDescriptions.add(
          aModel.listStatements((org.apache.jena.rdf.model.Resource) node, SCHEMA.location, (RDFNode) null)
        );
        String describeLocations = String.format(
          "DESCRIBE ?location WHERE { <%1$s> <http://schema.org/location> ?location }", node
        );
        try (QueryExecution queryExecution = QueryExecutionFactory
          .create(QueryFactory.create(describeLocations), aModel)) {
          queryExecution.execDescribe(identifyingDescriptions);
        }
      }
    }
    return identifyingDescriptions;
  }

  public static Model getConciseBoundedDescription(String aId, Model aModel) {
    Model conciseBoundedDescription = ModelFactory.createDefaultModel();

    // Validate URI
    try {
      new URI(aId);
    } catch (URISyntaxException e) {
      return conciseBoundedDescription;
    }

    String describeStatement = String.format(CONCISE_BOUNDED_DESCRIPTION, aId);
    aModel.enterCriticalSection(Lock.READ);
    try (QueryExecution queryExecution = QueryExecutionFactory
      .create(QueryFactory.create(describeStatement), aModel)) {
      queryExecution.execDescribe(conciseBoundedDescription);
    } catch (InternalErrorException e){
      Logger.error("Error loading CBD", e);
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
    // The incoming model
    Model incoming = ModelFactory.createDefaultModel();
    try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
      aResource.reduce().toString().getBytes())) {
      RDFDataMgr.read(incoming, byteArrayInputStream, Lang.JSONLD);
    } catch (IOException e) {
      throw new RuntimeIOException(e);
    }
    // Reduce incoming model to CBD
    Model model = getConciseBoundedDescription(aResource.getId(), incoming);
    mBroaderConceptEnricher.enrich(model);

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

  private Commit.Diff getDiffs(@Nonnull Resource aResource) {
    List<Resource> resources = new ArrayList<>();
    try {
      resources = ResourceFramer.flatten(aResource, mConfiguration.getString("jsonld.context"));
    } catch (IOException e) {
      Logger.error("Failed to flatten resource", e);
    }
    TripleCommit.Diff diff = new TripleCommit.Diff();
    for (Resource resource : resources) {
      diff.append(getDiff(resource));
    }
    return diff;
  }

  Commit.Diff getDiffs(@Nonnull List<Resource> aResources) {
    Commit.Diff diff = new TripleCommit.Diff();
    for (Resource resource : aResources) {
      diff.append(getDiffs(resource));
    }
    return diff;
  }

  @Override
  public Resource deleteResource(@Nonnull String aId, Map<String, String> aMetadata)
    throws IOException {

    // Current data, outbound links
    Model dbstate = getConciseBoundedDescription(aId, mDb);

    // Current data, inbound links
    String constructStatement = String.format(CONSTRUCT_BACKLINKS, aId);
    mDb.enterCriticalSection(Lock.READ);
    try (QueryExecution queryExecution = QueryExecutionFactory
      .create(QueryFactory.create(constructStatement), mDb)) {
      queryExecution.execConstruct(dbstate);
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
    TripleCommit.Header header = new TripleCommit.Header(
      aMetadata.get(TripleCommit.Header.AUTHOR_HEADER),
      ZonedDateTime.parse(aMetadata.get(TripleCommit.Header.DATE_HEADER)));
    TripleCommit commit = new TripleCommit(header, diff);
    mGraphHistory.add(commit);

    return ResourceFramer.resourceFromModel(dbstate, aId, mConfiguration.getString("jsonld.context"));
  }

  @Override
  public List<Commit> log(String aId) {
    if (StringUtils.isEmpty(aId)) {
      return mGraphHistory.log();
    } else {
      return mGraphHistory.log(aId);
    }
  }

  public String sparql(String q) {

    String result;
    StringWriter out;

    mDb.enterCriticalSection(Lock.READ);
    try (QueryExecution queryExecution = QueryExecutionFactory
      .create(QueryFactory.create(q), mDb)) {
      switch (queryExecution.getQuery().getQueryType()) {
        case Query.QueryTypeSelect:
          ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
          ResultSetFormatter.outputAsJSON(byteArrayOutputStream, queryExecution.execSelect());
          result = byteArrayOutputStream.toString();
          break;
        case Query.QueryTypeConstruct:
          out = new StringWriter();
          queryExecution.execConstruct().write(out, "TURTLE");
          result = out.toString();
          break;
        case Query.QueryTypeDescribe:
          out = new StringWriter();
          queryExecution.execDescribe().write(out, "TURTLE");
          result = out.toString();
          break;
        default:
          result = "";
      }
    } finally {
      mDb.leaveCriticalSection();
    }
    return result;
  }

  /**
   * This method creates a diff using the SPARQL INSERT/DELETE semantics:
   * https://www.w3.org/TR/2010/WD-sparql11-update-20100126/#t413
   *
   * @param delete Construct statement for triples to delete
   * @param insert Construct statement for triples to insert
   * @param where Clause for the construct statement
   * @return The resulting diff
   */
  public Commit.Diff update(String delete, String insert, String where) {

    String constructQueryTemplate = "CONSTRUCT { %s } WHERE { %s }";
    TripleCommit.Diff diff = new TripleCommit.Diff();

    if (!StringUtils.isEmpty(where) && !StringUtils.isEmpty(delete)) {
      String deleteQuery = String.format(constructQueryTemplate, delete, where);
      Model deleteModel;
      mDb.enterCriticalSection(Lock.READ);
      try (QueryExecution queryExecution = QueryExecutionFactory
        .create(QueryFactory.create(deleteQuery), mDb)) {
        deleteModel = queryExecution.execConstruct();
      } finally {
        mDb.leaveCriticalSection();
      }
      StmtIterator itDelete = deleteModel.listStatements();
      while (itDelete.hasNext()) {
        Statement statement = itDelete.next();
        if (mDb.contains(statement)) {
          diff.removeStatement(statement);
        }
      }
    }

    if (!StringUtils.isEmpty(where) && !StringUtils.isEmpty(insert)) {
      String insertQuery = String.format(constructQueryTemplate, insert, where);
      Model insertModel;
      mDb.enterCriticalSection(Lock.READ);
      try (QueryExecution queryExecution = QueryExecutionFactory
        .create(QueryFactory.create(insertQuery), mDb)) {
        insertModel = queryExecution.execConstruct();
      } finally {
        mDb.leaveCriticalSection();
      }
      StmtIterator itInsert = insertModel.listStatements();
      while (itInsert.hasNext()) {
        Statement statement = itInsert.next();
        if (!mDb.contains(statement)) {
          diff.addStatement(statement);
        }
      }
    }
    return diff;
  }

  public String label(String aId) {
    String labelQuery = String.format(LABEL_RESOURCE, aId);
    String result = aId;

    mDb.enterCriticalSection(Lock.READ);
    try (QueryExecution queryExecution = QueryExecutionFactory
      .create(QueryFactory.create(labelQuery), mDb)) {
      ResultSet resultSet = queryExecution.execSelect();
      while (resultSet.hasNext()) {
        QuerySolution querySolution = resultSet.next();
        result = querySolution.get("name").asLiteral().getString();
      }
    } finally {
      mDb.leaveCriticalSection();
    }

    return result;
  }
}
