package services;

import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.shared.Lock;
import org.apache.jena.atlas.RuntimeIOException;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by fo on 27.04.16.
 */
public class InverseEnricher implements ResourceEnricher {

  private final Model mInverseRelations;

  public static final String CONSTRUCT_INVERSE =
    "CONSTRUCT {?o <%1$s> ?s} WHERE {" +
    "  ?s <%2$s> ?o ." +
    "}";


  public InverseEnricher() {

    this.mInverseRelations = ModelFactory.createDefaultModel();
    try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("inverses.ttl")) {
      RDFDataMgr.read(mInverseRelations, inputStream, Lang.TURTLE);
    } catch (IOException e) {
      throw new RuntimeIOException(e);
    }

  }

  public void enrich(Model aToBeEnriched) {

    Model inverses = ModelFactory.createDefaultModel();

    mInverseRelations.enterCriticalSection(Lock.READ);
    aToBeEnriched.enterCriticalSection(Lock.READ);
    try {
      for (Statement stmt : mInverseRelations.listStatements().toList()) {
        String inferConstruct = String.format(CONSTRUCT_INVERSE, stmt.getSubject(), stmt.getObject());
        QueryExecutionFactory.create(QueryFactory.create(inferConstruct), aToBeEnriched).execConstruct(inverses);
      }
    } finally {
      aToBeEnriched.leaveCriticalSection();
      mInverseRelations.leaveCriticalSection();
    }

    aToBeEnriched.enterCriticalSection(Lock.WRITE);
    try {
      aToBeEnriched.add(inverses);
    } finally {
      aToBeEnriched.leaveCriticalSection();
    }

  }
}
