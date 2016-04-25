package services;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.shared.Lock;
import models.Commit;
import models.GraphHistory;
import models.Resource;
import models.TripleCommit;
import play.Logger;
import services.repository.Writable;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by fo on 23.03.16.
 */
public class ResourceIndexer {

  Model mDb;
  Writable mTargetRepo;
  GraphHistory mGraphHistory;

  private final static String SCOPE_QUERY_TEMPLATE =
    "SELECT DISTINCT ?s1 ?s2 WHERE {" +
    "    ?s1 ?p1 <%1$s> ." +
    "    OPTIONAL { ?s2 ?p2 ?s1 . } ." +
    "}";

  public ResourceIndexer(Model aDb, Writable aTargetRepo, GraphHistory aGraphHistory) {

    this.mDb = aDb;
    this.mTargetRepo = aTargetRepo;
    this.mGraphHistory = aGraphHistory;

  }

  public Set<String> getScope(Commit.Diff aDiff) {

    Set<String> commitScope = new HashSet<>();
    Set<String> indexScope = new HashSet<>();

    if (aDiff.getLines().isEmpty()) {
      return commitScope;
    }

    for (Commit.Diff.Line line : aDiff.getLines()) {
      RDFNode subject = ((TripleCommit.Diff.Line)line).stmt.getSubject();
      RDFNode object = ((TripleCommit.Diff.Line)line).stmt.getObject();
      if (subject.isURIResource()) {
        commitScope.add(subject.toString());
      }
      if (object.isURIResource()) {
        commitScope.add(object.toString());
      }
    }

    indexScope.addAll(commitScope);

    mDb.enterCriticalSection(Lock.READ);
    try {
      for (String id : commitScope) {
        String query = String.format(SCOPE_QUERY_TEMPLATE, id);
        try (QueryExecution queryExecution = QueryExecutionFactory.create(QueryFactory.create(query), mDb)) {
          ResultSet rs = queryExecution.execSelect();
          while (rs.hasNext()) {
            QuerySolution qs = rs.next();
            if (qs.contains("s1")) {
              indexScope.add(qs.get("s1").toString());
            }
            if (qs.contains("s2")) {
              indexScope.add(qs.get("s2").toString());
            }
          }
        } catch (QueryParseException e) {
          Logger.error(query);
        }
      }
    } finally {
      mDb.leaveCriticalSection();
    }

    Logger.debug("Indexing scope is " + indexScope);

    return indexScope;

  }

  public Set<Resource> getResources(Commit.Diff aDiff) {

    Set<Resource> resourcesToIndex = new HashSet<>();
    Set<String> idsToIndex = this.getScope(aDiff);
    for (String id : idsToIndex) {
      try {
        Resource resource = ResourceFramer.resourceFromModel(mDb, id);
        if (resource != null) {
          resourcesToIndex.add(resource);
        }
      } catch (IOException e) {
        Logger.error("Could not create resource from model", e);
      }
    }

    return resourcesToIndex;

  }

  public void index(Commit commit) {
    Commit.Diff diff = commit.getDiff();
    index(diff);
  }

  public void index(Commit.Diff diff) {
    long startTime = System.nanoTime();
    Set<Resource> denormalizedResources = getResources(diff);
    for (Resource dnr : denormalizedResources) {
      if (dnr.hasId()) {
        try {
          Map<String, String> metadata;
          if (mGraphHistory != null) {
            metadata = mGraphHistory.log(dnr.getId()).get(0).getHeader().toMap();
          } else {
            metadata = new HashMap<>();
          }
          mTargetRepo.addResource(dnr, metadata);
        } catch (IndexOutOfBoundsException | IOException e) {
          Logger.error("Could not index commit", e);
        }
      }
    }
    long endTime = System.nanoTime();
    long duration = (endTime - startTime) / 1000000000;
    Logger.debug("Done indexing, took ".concat(Long.toString(duration)).concat(" sec."));
  }

}
