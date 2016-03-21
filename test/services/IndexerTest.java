package services;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Sets;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import helpers.JsonTest;
import models.Resource;
import models.TripleCommit;
import org.apache.commons.io.IOUtils;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by fo on 11.02.16.
 */
public class IndexerTest implements JsonTest {

  private class Indexer {

    Model mDb;

    private final static String SCOPE_QUERY =
      "SELECT DISTINCT ?s1 ?s2 WHERE {" +
      "    ?s1 ?p1 ?s ." +
      "    OPTIONAL { ?s2 ?p2 ?s1 } ." +
      "    FILTER (%1$s) ." +
      "}";

    public Indexer(Model aDb) {
      this.mDb = aDb;
    }

    public Set<String> getScope(TripleCommit.Diff aDiff) {
      Set<String> scope = new HashSet<>();
      for (TripleCommit.Diff.Line line : aDiff.getLines()) {
        RDFNode subject = line.stmt.getSubject();
        RDFNode object = line.stmt.getObject();
        if (subject.isURIResource()) {
          scope.add(subject.toString());
        }
        if (object.isURIResource()) {
          scope.add(object.toString());
        }
      }
      String filter = String.join(" || ", scope.stream().map(id -> "?s = <".concat(id).concat(">"))
        .collect(Collectors.toSet()));
      ResultSet rs = QueryExecutionFactory.create(QueryFactory.create(String.format(SCOPE_QUERY, filter)), mDb)
        .execSelect();
      while (rs.hasNext()) {
        QuerySolution qs = rs.next();
        if (qs.contains("s1")) {
          scope.add(qs.get("s1").toString());
        }
        if (qs.contains("s2")) {
          scope.add(qs.get("s2").toString());
        }
      }
      return scope;
    }

  }

  //private Config config = ConfigFactory.load(ClassLoader.getSystemClassLoader(), "test.conf");

  @Test
  public void testIndexNewResourceWithNewReference() throws IOException {

    // The model for the indexer to SPARQL against
    Model db = ModelFactory.createDefaultModel();

    // We assume the diff has already been applied when indexing, so populate model
    Resource resource1 = getResourceFromJsonFile(
      "IndexerTest/testNewResourceWithNewReference.IN.json");
    RDFDataMgr.read(db, new ByteArrayInputStream(resource1.toString().getBytes(StandardCharsets.UTF_8)), Lang.JSONLD);

    // Calculate scope of resources to be (re-)indexed
    String diffString = IOUtils.toString(
      ClassLoader.getSystemResourceAsStream("IndexerTest/testNewResourceWithNewReference.IN.ndiff"), "UTF-8");
    Indexer indexer = new Indexer(db);
    TripleCommit.Diff diff = TripleCommit.Diff.fromString(diffString);
    Set<String> idsToIndex = indexer.getScope(diff);
    assertEquals(4, idsToIndex.size());
    // FIXME: limit scope of ids to those present in our database...
    Set expected = Sets.newHashSet("http://schema.org/Article", "http://schema.org/Person", "info:456", "info:123");
    assertEquals(expected, idsToIndex);
    //TriplestoreRepository triplestoreRepository = new TriplestoreRepository(config, db);

    //System.out.println(triplestoreRepository.getDiff(resource1));
  }

}
