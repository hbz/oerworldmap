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
import models.Record;
import models.Resource;
import models.TripleCommit;
import play.Logger;
import services.repository.Writable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by fo on 23.03.16.
 */
public class ResourceIndexer {

  Model mDb;
  Writable mTargetRepo;

  private final static String SCOPE_QUERY_TEMPLATE =
    "SELECT DISTINCT ?s1 ?s2 WHERE {" +
    "    ?s1 ?p1 ?s ." +
    "    FILTER (%1$s) ." +
    "    OPTIONAL { ?s2 ?p2 ?s1 . } ." +
    "}";

  public ResourceIndexer(Model aDb, Writable aTargetRepo) {

    this.mDb = aDb;
    this.mTargetRepo = aTargetRepo;

  }

  public Set<String> getScope(Commit.Diff aDiff) {

    Set<String> scope = new HashSet<>();

    if (aDiff.getLines().isEmpty()) {
      return scope;
    }

    for (Commit.Diff.Line line : aDiff.getLines()) {
      RDFNode subject = ((TripleCommit.Diff.Line)line).stmt.getSubject();
      RDFNode object = ((TripleCommit.Diff.Line)line).stmt.getObject();
      if (subject.isURIResource()) {
        scope.add(subject.toString());
      }
      if (object.isURIResource()) {
        scope.add(object.toString());
      }
    }

    String filter = String.join(" || ", scope.stream().map(id -> "?s = <".concat(id).concat(">"))
      .collect(Collectors.toSet()));

    String query = String.format(SCOPE_QUERY_TEMPLATE, filter);

    mDb.enterCriticalSection(Lock.READ);
    try {
      try (QueryExecution queryExecution = QueryExecutionFactory.create(QueryFactory.create(query), mDb)) {
        ResultSet rs = queryExecution.execSelect();
        while (rs.hasNext()) {
          QuerySolution qs = rs.next();
          if (qs.contains("s1")) {
            scope.add(qs.get("s1").toString());
          }
          if (qs.contains("s2")) {
            scope.add(qs.get("s2").toString());
          }
        }
      } catch (QueryParseException e) {
        Logger.error(query);
      }
    } finally {
      mDb.leaveCriticalSection();
    }

    Logger.debug("Indexing scope is " + scope);

    return scope;

  }

  public Set<Resource> getResources(Commit.Diff aDiff) {

    Set<Resource> resourcesToIndex = new HashSet<>();
    Set<String> idsToIndex = this.getScope(aDiff);
    for (String id : idsToIndex) {
      try {
        Resource resource  = ResourceFramer.resourceFromModel(mDb, id);
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
    Logger.debug("Indexing");
    Set<Resource> denormalizedResources = getResources(diff);
    Map<String, String> metadata = commit.getHeader().toMap();
    List<Resource> resourcesToIndex = new ArrayList<>();
    for (Resource dnr : denormalizedResources) {
      if (dnr.hasId()) {
        resourcesToIndex.add(dnr);
      }
    }
    if (!resourcesToIndex.isEmpty()) {
      try {
        mTargetRepo.addResources(resourcesToIndex, metadata);
      } catch (IOException e) {
        Logger.error("Could not index commit", e);
      }
    }
    Logger.debug("Done indexing");
  }

}
