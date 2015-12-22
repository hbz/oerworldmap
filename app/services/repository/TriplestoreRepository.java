package services.repository;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.typesafe.config.Config;
import controllers.Global;
import models.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import play.Logger;
import play.Play;

import javax.annotation.Nonnull;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by fo on 10.12.15.
 */
public class TriplestoreRepository extends Repository
  implements Readable, Writable {

  public class Diff {

    private List<Line> lines = new ArrayList<>();

    private class Line {

      public final boolean add;
      public final boolean remove;
      public final Statement stmt;

      public Line(Statement stmt, boolean add) {
        this.add = add;
        this.remove = !add;
        this.stmt = stmt;
      }

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
    "  <%1$s> ?p ?o OPTIONAL{?o ?pp ?oo}" +
    "}";

  public static final String DESCRIBE_DBSTATE = "DESCRIBE <%s>";

  private PrintWriter diffSink;

  private Git git;

  private Model db;

  private final String diffDir;

  private final String diffFile ;

  public TriplestoreRepository(Config aConfiguration) {
    this(aConfiguration, ModelFactory.createDefaultModel());
  }

  public TriplestoreRepository(Config aConfiguration, Model aModel) {
    super(aConfiguration);
    this.diffDir = aConfiguration.getString("repo.triplestore.diffDir");
    this.diffFile = aConfiguration.getString("repo.triplestore.diffFile");
    this.db = aModel;

    try {
      this.diffSink = new PrintWriter(new BufferedWriter(new FileWriter(Paths.get(diffDir, diffFile).toFile(), true)));
    } catch (IOException e) {
      Logger.error(e.toString());
      this.diffSink = new PrintWriter(new BufferedWriter(new StringWriter()));
    }
    try {
      this.git = openOrCreate(new File(diffDir));
    } catch (IOException | GitAPIException e) {
      Logger.error(e.toString());
    }
  }

  @Override
  public Resource getResource(@Nonnull String aId) throws IOException {

    // Current data
    String describeStatement = String.format(DESCRIBE_RESOURCE, aId);
    Model dbstate = ModelFactory.createDefaultModel();
    QueryExecutionFactory.create(QueryFactory.create(describeStatement), db).execDescribe(dbstate);

    return resourceFromModel(dbstate);

  }


  public Resource checkoutResource(@Nonnull String aId, TriplestoreRepository.Diff diff) throws IOException {

    // Current data
    String describeStatement = String.format(DESCRIBE_RESOURCE, aId);
    Model dbstate = ModelFactory.createDefaultModel();
    QueryExecutionFactory.create(QueryFactory.create(describeStatement), db).execDescribe(dbstate);

    // Unapply diff
    diff.unapply(dbstate);

    return resourceFromModel(dbstate);

  }

  @Override
  public List<Resource> getAll(@Nonnull String aType) throws IOException {
    return null;
  }

  @Override
  public void addResource(@Nonnull Resource aResource, @Nonnull String aType) throws IOException {

    addResource(aResource);

  }

  @SuppressWarnings("unchecked")
  private Resource resourceFromModel(Model model) throws IOException {

    // Create resource from framed jsonld via nquads
    ByteArrayOutputStream nquads = new ByteArrayOutputStream();
    RDFDataMgr.write(nquads, model, Lang.NQUADS);
    try {
      Object frame = JsonUtils.fromInputStream(ClassLoader.getSystemResourceAsStream("public/json/frame.json"));
      Object jsonld = JsonLdProcessor.fromRDF(new String(nquads.toByteArray(), StandardCharsets.UTF_8));
      Map<String, Object> framed = JsonLdProcessor.frame(jsonld, frame, new JsonLdOptions());
      Map<String, Object> graph = (Map) ((List) framed.get("@graph")).get(0);
      // FIXME: add proper @context
      return Resource.fromMap(graph);
    } catch (JsonLdError e) {
      Logger.error(e.toString());
      return null;
    }

  }

  public TriplestoreRepository.Diff addResource(@Nonnull Resource aResource) throws IOException {

    TriplestoreRepository.Diff diff = getDiff(aResource);
    diff.apply(db);
    diffSink.println(diff.toString());
    diffSink.flush();
    if (git != null) {
      try {
        git.add().addFilepattern(diffFile).call();
        git.commit().setMessage("Updated ".concat(aResource.getId()).concat(" with\n").concat(diff.toString())).call();
      } catch (GitAPIException e) {
        Logger.error(e.toString());
      }
    }
    return diff;

  }

  private TriplestoreRepository.Diff getDiff(@Nonnull Resource aResource) {

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

    return diff;

  }

  @Override
  public Resource deleteResource(@Nonnull String aId) throws IOException {
    return null;
  }

  // http://stackoverflow.com/questions/24620634/jgit-how-to-open-a-repo-creating-it-if-it-does-not-exist
  static Git openOrCreate( File gitDirectory ) throws IOException, GitAPIException {
    Git git;
    FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
    repositoryBuilder.addCeilingDirectory( gitDirectory );
    repositoryBuilder.findGitDir( gitDirectory );
    if( repositoryBuilder.getGitDir() == null ) {
      git = Git.init().setDirectory( gitDirectory ).call();
    } else {
      git = new Git( repositoryBuilder.build() );
    }
    return git;
  }

}
