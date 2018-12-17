package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jsonldjava.core.JsonLdOptions;
import helpers.JsonLdConstants;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.vocabulary.RDF;
import play.Logger;

/**
 * Created by fo on 23.03.16.
 */
public class ResourceFramer {

  private static final ObjectMapper mObjectMapper = new ObjectMapper();

  private static WriterDatasetRIOT mWriter = RDFDataMgr.createDatasetWriter(RDFFormat.JSONLD_FRAME_PRETTY);

  public static Resource resourceFromModel(Model aModel, String aId, String aContextUrl) throws IOException {

    NodeIterator types = aModel.listObjectsOfProperty(aModel.createResource(aId), RDF.type);

    if (!types.hasNext()) {
      return null;
    }

    // JSON-LD context
    Map<String, String> jsonLdContext = new HashMap<>();
    jsonLdContext.put("@context", aContextUrl);

    // JSON-LD frame
    Map<String, String> frame = new HashMap<>();
    frame.put("@context", aContextUrl);
    frame.put("@embed", "@always");
    frame.put("@type", types.next().toString());

    // Jena write context config
    JsonLdOptions jsonLdOptions = new JsonLdOptions();
    jsonLdOptions.setUseNativeTypes(true);
    jsonLdOptions.setOmitGraph(false);
    JsonLDWriteContext jenaWriteContext = new JsonLDWriteContext();
    jenaWriteContext.setJsonLDContext(jsonLdContext);
    jenaWriteContext.setFrame(frame);
    jenaWriteContext.setOptions(jsonLdOptions);

    // Serialize to JSON-LD and deserialize to JsonNode
    ByteArrayOutputStream boas = new ByteArrayOutputStream();
    DatasetGraph g = DatasetFactory.create(aModel).asDatasetGraph();
    mWriter.write(boas, g, RiotLib.prefixMap(g), null, jenaWriteContext);
    JsonNode jsonNode = mObjectMapper.readTree(boas.toByteArray());
    ArrayNode graphs = (ArrayNode) jsonNode.get(JsonLdConstants.GRAPH);
    for (JsonNode graph : graphs) {
      if (graph.get(JsonLdConstants.ID).asText().equals(aId)) {
        ObjectNode result = (ObjectNode) graph;
        result.put(JsonLdConstants.CONTEXT, aContextUrl);
        Logger.debug("Framed " + aId);
        return Resource.fromJson(prune(result));
      }
    }

    return null;
  }

  private static ObjectNode prune(ObjectNode node) {
    ObjectNode result = JsonNodeFactory.instance.objectNode();
    Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
    while(fields.hasNext()) {
      Map.Entry<String, JsonNode> entry = fields.next();
      JsonNode value = entry.getValue();
      String key = entry.getKey();
      if (value.isArray()) {
        result.set(key, prune((ArrayNode) value));
      } else if (value.isObject()) {
        result.set(key, prune((ObjectNode) value));
      } else if (!value.isTextual()|| (value.isTextual() && !value.asText().startsWith("_:"))) {
        result.set(key, value);
      }
    }
    return result;
  }

  private static ArrayNode prune(ArrayNode node) {
    ArrayNode result = JsonNodeFactory.instance.arrayNode();
    for (JsonNode entry : node) {
      if (entry.isArray()) {
        result.add(prune((ArrayNode) entry));
      } else if (entry.isObject()) {
        result.add(prune((ObjectNode) entry));
      } else {
        result.add(entry);
      }
    }
    return result;
  }

  public static List<Resource> flatten(Resource aResource, String aContextUrl) throws IOException {

    Model model = ModelFactory.createDefaultModel();
    RDFDataMgr
      .read(model, IOUtils.toInputStream(aResource.toString(), StandardCharsets.UTF_8), Lang.JSONLD);
    List<Resource> resources = new ArrayList<>();

    String subjectsQuery = "SELECT DISTINCT ?s WHERE { ?s ?p ?o . FILTER isIRI(?s) }";
    try (QueryExecution queryExecution = QueryExecutionFactory
      .create(QueryFactory.create(subjectsQuery), model)) {
      ResultSet resultSet = queryExecution.execSelect();
      while (resultSet.hasNext()) {
        QuerySolution querySolution = resultSet.next();
        String subject = querySolution.get("s").toString();
        resources.add(resourceFromModel(model, subject, aContextUrl));
      }
    }

    return resources;
  }
}
