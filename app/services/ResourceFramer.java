package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import helpers.JsonLdConstants;
import models.Resource;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.WriterDatasetRIOT;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.shared.Lock;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.vocabulary.RDF;
import play.Logger;
import services.repository.TriplestoreRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Created by fo on 23.03.16.
 */
public class ResourceFramer {

  public static Resource resourceFromModel(Model aModel, String aId) throws IOException {

    String describeStatement = String.format(TriplestoreRepository.EXTENDED_DESCRIPTION, aId);
    Model dbstate = ModelFactory.createDefaultModel();

    aModel.enterCriticalSection(Lock.READ);
    dbstate.enterCriticalSection(Lock.WRITE);
    try {
      try (QueryExecution queryExecution = QueryExecutionFactory.create(QueryFactory.create(describeStatement),
          aModel)) {
        queryExecution.execDescribe(dbstate);

        ByteArrayOutputStream unframed = new ByteArrayOutputStream();
        RDFDataMgr.write(unframed, dbstate, RDFFormat.JSONLD_EXPAND_PRETTY);
        unframed.close();

        NodeIterator types = aModel.listObjectsOfProperty(aModel.createResource(aId), RDF.type);

        if (types.hasNext()) {
          DatasetGraph g = DatasetFactory.create(dbstate).asDatasetGraph();
          JsonLDWriteContext ctx = new JsonLDWriteContext();
          String context = "{ \"@context\": \"https://oerworldmap.org/assets/json/context.json\"}";
          ctx.setJsonLDContext(context);
          ByteArrayOutputStream boas = new ByteArrayOutputStream();
          WriterDatasetRIOT w = RDFDataMgr.createDatasetWriter(RDFFormat.JSONLD_COMPACT_PRETTY);
          w.write(boas, g, RiotLib.prefixMap(g), null, ctx);
          ObjectMapper objectMapper = new ObjectMapper();
          JsonNode jsonNode = objectMapper.readTree(boas.toByteArray());

          if (jsonNode.has(JsonLdConstants.GRAPH)) {
            ArrayNode graphs = (ArrayNode) jsonNode.get(JsonLdConstants.GRAPH);
            for (JsonNode graph : graphs) {
              if (graph.get(JsonLdConstants.ID).asText().equals(aId)) {
                ObjectNode result = (ObjectNode) buildTree(graph, graphs);
                result.put(JsonLdConstants.CONTEXT, "https://oerworldmap.org/assets/json/context.json");
                Logger.debug("Framed " + aId);
                return Resource.fromJson(result);
              }
            }
          } else {
            ObjectNode result = (ObjectNode) jsonNode;
            result.put(JsonLdConstants.CONTEXT, "https://oerworldmap.org/assets/json/context.json");
            Logger.debug("Framed " + aId);
            return Resource.fromJson(result);
          }
        }
      }
    } finally {
      dbstate.leaveCriticalSection();
      aModel.leaveCriticalSection();
    }

    return null;

  }

  private static JsonNode buildTree(JsonNode graph, ArrayNode graphs) {
    if (graph.isArray()) {
      ArrayNode result = new ArrayNode(JsonNodeFactory.instance);
      Iterator<JsonNode> elements = graph.elements();
      while (elements.hasNext()) {
        JsonNode element = elements.next();
        result.add(buildTree(element, graphs));
      }
      return result;
    } else if (graph.isObject()) {
      ObjectNode result = new ObjectNode(JsonNodeFactory.instance);
      Iterator<String> fieldNames = graph.fieldNames();
      while(fieldNames.hasNext()) {
        String fieldName = fieldNames.next();
        if (! (fieldName.equals(JsonLdConstants.ID) && graph.get(fieldName).asText().startsWith("_:"))) {
          result.set(fieldName, link(graph.get(fieldName), graphs));
        }
      }
      return result;
    } else {
      return graph;
    }
  }

  private static JsonNode link(JsonNode ref, ArrayNode graphs) {
    JsonNode graph;
    if (ref.has(JsonLdConstants.ID)) {
      graph = getGraph(ref.get(JsonLdConstants.ID).asText(), graphs);
    } else {
      graph = ref;
    }
    List<String> linkProperties = Arrays.asList("@id", "@type", "@value", "@language", "name", "image", "location",
      "startDate", "endDate");
    if (graph != null && graph.isArray()) {
      ArrayNode result = new ArrayNode(JsonNodeFactory.instance);
      Iterator<JsonNode> elements = graph.elements();
      while (elements.hasNext()) {
        JsonNode element = elements.next();
        result.add(link(element, graphs));
      }
      return result;
    } else if (graph != null && graph.isObject()) {
      if (graph.get(JsonLdConstants.ID) != null && graph.get(JsonLdConstants.ID).asText().startsWith("_:")) {
        return buildTree(graph, graphs);
      }
      ObjectNode result = new ObjectNode(JsonNodeFactory.instance);
      Iterator<String> fieldNames = graph.fieldNames();
      while(fieldNames.hasNext()) {
        String fieldName = fieldNames.next();
        if (linkProperties.contains(fieldName)) {
          JsonNode value = graph.get(fieldName);
          if (value.isObject() || value.isArray()) {
            result.set(fieldName, link(value, graphs));
          } else {
            result.set(fieldName, value);
          }
        }
      }
      return result;
    } else {
      return graph;
    }
  }

  private static JsonNode getGraph(String aId, ArrayNode graphs) {
    for (JsonNode graph: graphs) {
      if (graph.get(JsonLdConstants.ID).asText().equals(aId)) {
        return graph;
      }
    }
    return new ObjectNode(JsonNodeFactory.instance);
  }

  public static List<Resource> flatten(Resource resource) throws IOException {

    Model model = ModelFactory.createDefaultModel();
    RDFDataMgr.read(model, IOUtils.toInputStream(resource.toString(), StandardCharsets.UTF_8), Lang.JSONLD);
    List<Resource> resources = new ArrayList<>();

    String subjectsQuery = "SELECT DISTINCT ?s WHERE { ?s ?p ?o . FILTER isIRI(?s) }";
    try (QueryExecution queryExecution = QueryExecutionFactory.create(QueryFactory.create(subjectsQuery), model)) {
      ResultSet resultSet = queryExecution.execSelect();
      while (resultSet.hasNext()) {
        QuerySolution querySolution = resultSet.next();
        String subject = querySolution.get("s").toString();
        resources.add(resourceFromModel(model, subject));
      }
    }

    return resources;

  }

}
