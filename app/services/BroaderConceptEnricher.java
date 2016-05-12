package services;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.shared.Lock;
import org.apache.jena.atlas.RuntimeIOException;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.eclipse.jetty.util.ConcurrentHashSet;

import helpers.JsonLdConstants;
import models.Resource;
import play.Logger;
import services.repository.Readable;

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
    try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
        .getResourceAsStream("public/json/esc.json")) {
      RDFDataMgr.read(mConceptSchemes, inputStream, Lang.JSONLD);
    } catch (IOException e) {
      throw new RuntimeIOException(e);
    }

    // Load ISCED-1997
    try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
        .getResourceAsStream("public/json/isced-1997.json")) {
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
              Statement broaderConcept = ResourceFactory.createStatement(stmt.getSubject(), stmt.getPredicate(),
                querySolution.get("broader").asResource());
              broaderConcepts.add(broaderConcept);
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
