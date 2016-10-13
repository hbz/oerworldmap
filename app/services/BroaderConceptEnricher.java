package services;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import helpers.SCHEMA;
import org.apache.jena.atlas.RuntimeIOException;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shared.Lock;

/**
 * @author fo, pvb
 */
public class BroaderConceptEnricher implements ResourceEnricher {

  private final Model mConceptSchemes;

  private static final String SELECT_BROADER =
    "SELECT ?broader WHERE {" +
    "  <%1$s> <http://schema.org/broader>+ ?broader " +
    "}";

  public BroaderConceptEnricher() {

    this.mConceptSchemes = ModelFactory.createDefaultModel();

    // Load ESC
    try{
      InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("public/json/esc.json");
      if (inputStream == null){
        inputStream = new FileInputStream("public/json/esc.json");
      }
      RDFDataMgr.read(mConceptSchemes, inputStream, Lang.JSONLD);
    } catch (IOException e) {
      throw new RuntimeIOException(e);
    }

    // Load ISCED-1997
    try {
      InputStream inputStream = Thread.currentThread().getContextClassLoader()
        .getResourceAsStream("public/json/isced-1997.json");
      if (inputStream == null) {
        inputStream = new FileInputStream("public/json/isced-1997.json");
      }
      RDFDataMgr.read(mConceptSchemes, inputStream, Lang.JSONLD);
    } catch (IOException e) {
      throw new RuntimeIOException(e);
    }

  }

  public void enrich(Model aToBeEnriched) {

    Model broaderConcepts = ModelFactory.createDefaultModel();

    aToBeEnriched.enterCriticalSection(Lock.READ);
    try {
      for (Statement stmt : aToBeEnriched.listStatements().toSet()) {
        if (stmt.getObject().isResource()) {
          try (QueryExecution queryExecution = QueryExecutionFactory.create(
              String.format(SELECT_BROADER, stmt.getObject()), mConceptSchemes)) {
            ResultSet resultSet = queryExecution.execSelect();
            while (resultSet.hasNext()) {
              QuerySolution querySolution = resultSet.next();
              if (!stmt.getPredicate().equals(SCHEMA.broader)) {
                Statement broaderConcept = ResourceFactory.createStatement(stmt.getSubject(), stmt.getPredicate(),
                  querySolution.get("broader").asResource());
                broaderConcepts.add(broaderConcept);
              }
            }
          }
        }
      }
      aToBeEnriched.add(broaderConcepts);
    } finally {
      aToBeEnriched.leaveCriticalSection();
    }

  }

}
