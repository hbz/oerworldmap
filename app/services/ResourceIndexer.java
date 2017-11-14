package services;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import helpers.Types;
import models.*;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.shared.Lock;
import play.Logger;
import services.repository.Writable;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Created by fo on 23.03.16.
 */
public class ResourceIndexer {

  private Model mDb;
  private Writable mTargetRepo;
  private GraphHistory mGraphHistory;
  private ResourceFramer mResourceFramer;

  private final static String GLOBAL_QUERY_TEMPLATE =
    "SELECT DISTINCT ?s WHERE {" +
    "    ?s a []" +
    "}";

  // TODO: evaluate if there are other properties to exclude from triggering indexing
  private final static String SCOPE_QUERY_TEMPLATE =
    "SELECT DISTINCT ?s1 WHERE {" +
    "    ?s1 ?p1 <%1$s> ." +
    "    FILTER ( ?p1 != <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> )" +
    "    OPTIONAL { ?y a <http://www.w3.org/2004/02/skos/core#Concept> . FILTER (<%1$s> = ?y) . }" +
    "    FILTER ( !BOUND(?y) ) " +
    "}";


  public ResourceIndexer(Model aDb, Writable aTargetRepo, GraphHistory aGraphHistory, final Types aTypes) {
    mDb = aDb;
    mTargetRepo = aTargetRepo;
    mGraphHistory = aGraphHistory;
    try {
      mResourceFramer = new ResourceFramer(aTypes);
    } catch (ProcessingException | IOException e) {
      Logger.error("Could not build types from types object: " + aTypes);
      e.printStackTrace();
    }
  }

  /**
   * Extracts resources that need to be indexed from a triple diff
   * @param aDiff The diff from which to extract resources
   * @return The list of resources touched by the diff
   */
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
    indexScope.addAll(getScope(commitScope));
    Logger.debug("Indexing scope is " + indexScope);
    return indexScope;
  }

  /**
   * Queries the triple store for related resources that must also be indexed
   * @param aIds The list of resources for which to find related resources
   * @return The list of related resources
   */
  public Set<String> getScope(Set<String> aIds) {

    Set<String> indexScope = new HashSet<>();
    mDb.enterCriticalSection(Lock.READ);
    try {
      for (String id : aIds) {
        indexScope.addAll(getScope(id));
      }
    } finally {
      mDb.leaveCriticalSection();
    }

    return indexScope;

  }

  /**
   * Queries the triple store for related resources that must also be indexed
   * @param aId A resource for which to find related resources
   * @return The list of related resources
   */
  public Set<String> getScope(String aId) {

    Set<String> indexScope = new HashSet<>();
    String query = String.format(SCOPE_QUERY_TEMPLATE, aId);
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
      Logger.error("Failed to execute query " + query, e);
    }
    return indexScope;
  }

  /**
   * Queries the triple store for all resources to be indexed
   * @return The list of typed resources
   */
  public Set<String> getScope() {
    Set<String> indexScope = new HashSet<>();
    String query = GLOBAL_QUERY_TEMPLATE;
    try (QueryExecution queryExecution = QueryExecutionFactory.create(QueryFactory.create(query), mDb)) {
      ResultSet rs = queryExecution.execSelect();
      while (rs.hasNext()) {
        QuerySolution qs = rs.next();
        if (qs.contains("s") && qs.get("s").isURIResource()) {
          indexScope.add(qs.get("s").toString());
        }
      }
    } catch (QueryParseException e) {
      Logger.error("Failed to execute query " + query, e);
    }
    Logger.debug("Indexing scope" + indexScope.toString());
    return indexScope;
  }

  public ModelCommon getItem(String aId) {
    try {
      ModelCommon resource = ResourceFramer.resourceFromModel(mDb, aId);
      if (resource != null) {
        return resource;
      }
    } catch (IOException e) {
      Logger.error("Could not create resource from model", e);
    }
    return null;
  }

  public Set<ModelCommon> getItems(String aId) {
    Set<ModelCommon> resourcesToIndex = new HashSet<>();
    Set<String> idsToIndex = this.getScope(aId);
    for (String id : idsToIndex) {
      resourcesToIndex.add(getItem(id));
    }
    return resourcesToIndex;
  }


  public Set<ModelCommon> getItems(Commit.Diff aDiff) {
    Set<ModelCommon> resourcesToIndex = new HashSet<>();
    Set<String> idsToIndex = this.getScope(aDiff);
    for (String id : idsToIndex) {
      resourcesToIndex.add(getItem(id));
    }
    return resourcesToIndex;
  }


  public Set<ModelCommon> getItems() {
    Set<ModelCommon> resourcesToIndex = new HashSet<>();
    Set<String> idsToIndex = this.getScope();
    for (String id : idsToIndex) {
      resourcesToIndex.add(getItem(id));
    }
    return resourcesToIndex;
  }

  public void index(ModelCommon aResource) {
    if (aResource.hasId()) {
      try {
        Map<String, Object> metadata = new HashMap<>();
        if (mGraphHistory != null) {
          List<Commit> history = mGraphHistory.log(aResource.getId());
          metadata.put(Record.CONTRIBUTOR, history.get(0).getHeader().getAuthor());
          metadata.put(Record.AUTHOR, history.get(history.size() - 1).getHeader().getAuthor());
          metadata.put(Record.DATE_MODIFIED, history.get(0).getHeader().getTimestamp()
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
          metadata.put(Record.DATE_CREATED, history.get(history.size() - 1).getHeader().getTimestamp()
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
        metadata.put(Record.LINK_COUNT, Integer.valueOf(aResource.getNumberOfSubFields("**.@id")));
        metadata.put(Record.LIKE_COUNT, String.valueOf(aResource.getAsList("objectIn").size()));
        mTargetRepo.addItem(aResource, metadata);
      } catch (IndexOutOfBoundsException | IOException e) {
        Logger.error("Could not index resource", e);
      }
    }
  }

  public void index(Set<ModelCommon> aResources) {

    long startTime = System.nanoTime();
    for (ModelCommon resource : aResources) {
      if (resource != null) {
        index(resource);
      }
    }
    long endTime = System.nanoTime();
    long duration = (endTime - startTime) / 1000000000;
    Logger.debug("Done indexing, took ".concat(Long.toString(duration)).concat(" sec."));

  }

  public void index(Commit.Diff aDiff) {
    Set<ModelCommon> denormalizedResources = getItems(aDiff);
    index(denormalizedResources);
  }

  public void index(String aId) {
    Set<ModelCommon> denormalizedResources;
    if (aId.equals("*")) {
      denormalizedResources = getItems();
    } else {
      denormalizedResources = getItems(aId);
    }
    index(denormalizedResources);
  }

}
