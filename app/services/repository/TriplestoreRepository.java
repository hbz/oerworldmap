package services.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.ning.http.client.AsyncHttpClientConfig;
import com.typesafe.config.Config;
import io.apigee.trireme.core.ScriptFuture;
import models.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import play.Logger;
import play.libs.F;
import play.libs.ws.ning.NingWSClient;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by fo on 10.12.15.
 */
public class TriplestoreRepository extends Repository
  implements Readable, Writable {

  public class Diff {

    private List<Line> lines = new ArrayList<>();

    public class Line {

      public final boolean add;
      public final boolean remove;
      public final Statement stmt;

      public Line(Statement stmt, boolean add) {
        this.add = add;
        this.remove = !add;
        this.stmt = stmt;
      }

    }

    public List<Line> getLines() {
      return this.lines;
    }

    public void addStatement(Statement stmt) {
      this.lines.add(new Line(stmt, true));
    }

    public void removeStatement(Statement stmt) {
      this.lines.add(new Line(stmt, false));
    }

    public void apply(Model model) {
      for (Line line : this.lines) {
        if (line.add) {
          model.add(line.stmt);
        } else {
          model.remove(line.stmt);
        }
      }
    }

    public void unapply(Model model) {
      for (Line line : this.lines) {
        if (line.add) {
          model.remove(line.stmt);
        } else {
          model.add(line.stmt);
        }
      }
    }

    public String toString() {
      String diffString = "";
      for (Line line : this.lines) {
        StringWriter triple = new StringWriter();
        RDFDataMgr.write(triple, ModelFactory.createDefaultModel().add(line.stmt), Lang.NTRIPLES);
        if (line.add) {
          diffString += "+ ".concat(triple.toString());
        } else {
          diffString += "- ".concat(triple.toString());
        }
      }
      return diffString;
    }

  }

  public static final String CONSTRUCT_INVERSE =
    "CONSTRUCT {?r <http://schema.org/knows> <%1$s> } WHERE {" +
    "  <%1$s> <http://schema.org/knows> ?r ." +
    "}";

  public static final String DESCRIBE_RESOURCE =
    "DESCRIBE <%1$s> ?o ?oo WHERE {" +
    "  <%1$s> ?p ?o FILTER isIRI(?o) OPTIONAL{?o ?pp ?oo FILTER isIRI(?oo)}" +
    "}";

  public static final String DESCRIBE_DBSTATE = "DESCRIBE <%s>";

  private Model db;

  private ScriptFuture framer;

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

    TriplestoreRepository.Diff diff = getDiff(aResource);
    diff.apply(db);

  }


  @SuppressWarnings("unchecked")
  private Resource resourceFromModel(Model model, String id) throws IOException {

    ByteArrayOutputStream nquads = new ByteArrayOutputStream();
    RDFDataMgr.write(nquads, model, Lang.NQUADS);

    AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();
    NingWSClient wsClient = new NingWSClient(builder.build());


    String type = URLEncoder.encode(model.listObjectsOfProperty(model.createResource(id), RDF.type).next().toString(),
      StandardCharsets.UTF_8.toString());

    F.Promise<JsonNode> promise = wsClient.url("http://localhost:8080/".concat(type))
      .setContentType("text/plain")
      .post(new String(nquads.toByteArray(), StandardCharsets.UTF_8))
      .map(response -> { return response.asJson(); });


    return Resource.fromJson(promise.get(1000));

  }

  public TriplestoreRepository.Diff getDiff(@Nonnull Resource aResource) {

    // The incoming model
    Model model = ModelFactory.createDefaultModel();
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(aResource.toString().getBytes());
    RDFDataMgr.read(model, byteArrayInputStream, Lang.JSONLD);

    // Add inferred (e.g. inverse) statements to incoming model
    String inferConstruct = String.format(CONSTRUCT_INVERSE, aResource.getId());
    QueryExecutionFactory.create(QueryFactory.create(inferConstruct), model).execConstruct(model);

    // Current data
    String describeStatement = String.format(DESCRIBE_DBSTATE, aResource.getId());
    Model dbstate = ModelFactory.createDefaultModel();
    QueryExecutionFactory.create(QueryFactory.create(describeStatement), db).execDescribe(dbstate);
    // Inverses in dbstate, or rather select them from DB?
    QueryExecutionFactory.create(QueryFactory.create(inferConstruct), dbstate).execConstruct(dbstate);

    Logger.debug(dbstate.toString());
    Logger.debug(model.toString());

    // Create diff
    Diff diff = new Diff();

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

}
