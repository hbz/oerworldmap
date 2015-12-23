package services.repository;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.typesafe.config.Config;
import models.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.StopWalkException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import play.Logger;

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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

  public static final String INDEX_SCOPE =
    "SELECT ?o ?oo WHERE {" +
    "  <%1$s> ?p ?o FILTER (isIRI(?o) && ?p != <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>)" +
    "  OPTIONAL{?o ?pp ?oo FILTER (isIRI(?oo) && ?p != <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>)}" +
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

  public void getLog(String aId) {
    RevWalk walk = new RevWalk(git.getRepository());
    walk.setRevFilter(new RevFilter() {
      @Override
      public boolean include(RevWalk revWalk, RevCommit revCommit) throws StopWalkException, MissingObjectException, IncorrectObjectTypeException, IOException {
        return true;
      }

      @Override
      public RevFilter clone() {
        return null;
      }
    });
    try {
      walk.markStart(walk.parseCommit(git.getRepository().resolve("HEAD")));
    } catch (Exception e) {
      Logger.error(e.toString());
    }

    for (RevCommit commit : walk) {
      System.out.print(commit.getFullMessage());
    }
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

  private Model modelFromResource(Resource resource) {
    Model model = ModelFactory.createDefaultModel();
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(resource.toString().getBytes());
    RDFDataMgr.read(model, byteArrayInputStream, Lang.JSONLD);
    return model;
  }

  public TriplestoreRepository.Diff addResource(@Nonnull Resource aResource) throws IOException {

    TriplestoreRepository.Diff diff = getDiff(aResource);
    diff.apply(db);
    diffSink.println(diff.toString());
    diffSink.flush();
    if (git != null) {
      try {
        git.add().addFilepattern(diffFile).call();
        git.commit().setMessage(diff.toString()).call();
      } catch (GitAPIException e) {
        Logger.error(e.toString());
      }
    }
    return diff;

  }

  public List<String> index(TriplestoreRepository.Diff diff) {

    List<String> resourceIds = new ArrayList<>();
    for (TriplestoreRepository.Diff.Line line : diff.getLines()) {
      String resourceId = line.stmt.getSubject().getURI();
      if (!resourceIds.contains(resourceId)) {
        resourceIds.add(resourceId);
      }
      String objectId = line.stmt.getSubject().getURI();
      if (!resourceIds.contains(objectId)) {
        resourceIds.add(objectId);
      }
    }

    List<String> scopeResourceIds = new ArrayList<>();
    scopeResourceIds.addAll(resourceIds);

    // All resources directly modified by the diff
    for (String resourceId : resourceIds) {
      try {
        Resource resource = getResource(resourceId);
        Model model = modelFromResource(resource);
        ResIterator it = model.listSubjects();
        // Resources indirectly modified by the diff due to denormalization
        while (it.hasNext()) {
          String id = it.next().toString();
          if (!scopeResourceIds.contains(id)) {
            scopeResourceIds.add(id);
          }
        }
      } catch (IOException e) {
        Logger.error(e.toString());
      }
    }

    return scopeResourceIds;

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
