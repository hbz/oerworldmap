package services.repository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.annotation.Nonnull;

import com.hp.hpl.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import com.fasterxml.jackson.databind.JsonNode;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.ning.http.client.AsyncHttpClientConfig;
import com.typesafe.config.Config;

import models.Resource;
import models.TripleDiff;
import play.Logger;
import play.libs.F;
import play.libs.ws.ning.NingWSClient;

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

  private Model db;

  public TriplestoreRepository(Config aConfiguration) {
    this(aConfiguration, ModelFactory.createDefaultModel());
  }

  public TriplestoreRepository(Config aConfiguration, Model aModel) {
    super(aConfiguration);
    this.db = aModel;
  }

  @Override
  public Resource getResource(@Nonnull String aId) throws IOException {

    // Current data
    String describeStatement = String.format(DESCRIBE_RESOURCE, aId);
    Model dbstate = ModelFactory.createDefaultModel();
    QueryExecutionFactory.create(QueryFactory.create(describeStatement), db).execDescribe(dbstate);
    Logger.debug("DBSTATE: " + dbstate);
    return resourceFromModel(dbstate, aId);

  }

  @Override
  public List<Resource> getAll(@Nonnull String aType) throws IOException {
    return null;
  }

  @Override
  public void addResource(@Nonnull Resource aResource, @Nonnull String aType) throws IOException {

    TripleDiff diff = getDiff(aResource);
    diff.apply(db);

  }

  private Resource resourceFromModel(Model model, String id) throws IOException {

    ByteArrayOutputStream nquads = new ByteArrayOutputStream();
    RDFDataMgr.write(nquads, model, Lang.NQUADS);

    AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();
    NingWSClient wsClient = new NingWSClient(builder.build());

    String type = URLEncoder.encode(
        model.listObjectsOfProperty(model.createResource(id), RDF.type).next().toString(),
        StandardCharsets.UTF_8.toString());

    F.Promise<JsonNode> promise = wsClient.url("http://localhost:8080/".concat(type))
        .setContentType("text/plain").post(new String(nquads.toByteArray(), StandardCharsets.UTF_8))
        .map(response -> {
          return response.asJson();
        });

    return Resource.fromJson(promise.get(1000));

  }

  public TripleDiff getDiff(@Nonnull Resource aResource) {

    // The incoming model
    Model model = ModelFactory.createDefaultModel();
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
        aResource.toString().getBytes());
    RDFDataMgr.read(model, byteArrayInputStream, Lang.JSONLD);

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
    TripleDiff diff = new TripleDiff();

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
    Model relations = ModelFactory.createDefaultModel();
    RDFDataMgr.read(relations, ClassLoader.getSystemResourceAsStream("inverses.ttl"), Lang.TURTLE);
    for (Statement stmt : relations.listStatements().toList()) {
      String inferConstruct = String.format(CONSTRUCT_INVERSE, stmt.getSubject(), stmt.getObject());
      QueryExecutionFactory.create(QueryFactory.create(inferConstruct), model).execConstruct(inverses);
    }
    model.add(inverses);
  }

}
