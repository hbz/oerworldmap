package services;

import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import helpers.JsonLdConstants;
import models.Record;
import models.Resource;
import models.TripleCommit;
import play.Logger;
import services.repository.Writable;

import java.io.IOException;
import java.util.HashSet;
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

    ResultSet rs = QueryExecutionFactory.create(QueryFactory.create(String.format(SCOPE_QUERY_TEMPLATE, filter)), mDb)
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

  public Set<Resource> getResources(TripleCommit.Diff aDiff) {

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

  public void index(TripleCommit.Diff diff) {
    Set<Resource> denormalizedResources = getResources(diff);
    for (Resource dnr : denormalizedResources) {
      if (dnr.hasId()) {
        String type = dnr.getAsString(JsonLdConstants.TYPE);
        try {
          mTargetRepo.addResource(new Record(dnr), new HashMap<>());
        } catch (IOException e) {
          Logger.error("Could not index commit", e);
        }
      }
    }
  }

}
