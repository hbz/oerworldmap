package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jsonldjava.core.JsonLdOptions;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.WriterDatasetRIOT;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.vocabulary.RDF;

/**
 * Created by fo on 23.03.16.
 */
public class ResourceFramer {

  private static final ObjectMapper mObjectMapper = new ObjectMapper();

  private static final org.apache.jena.rdf.model.Resource recordType = ResourceFactory
    .createResource("urn:uuid:".concat(UUID.randomUUID().toString()));

  private static final org.apache.jena.rdf.model.Resource recordId = ResourceFactory
    .createResource("urn:uuid:".concat(UUID.randomUUID().toString()));

  private static final org.apache.jena.rdf.model.Property resourceLink = ResourceFactory
    .createProperty("urn:uuid:".concat(UUID.randomUUID().toString()));

  private static WriterDatasetRIOT mWriter = RDFDataMgr.createDatasetWriter(RDFFormat.JSONLD_FRAME_PRETTY);

  public static Resource resourceFromModel(Model aModel, String aId, String aContextUrl) throws IOException {

    org.apache.jena.rdf.model.Resource resource = ResourceFactory.createResource(aId);

    if (!aModel.containsResource(resource)) {
      return null;
    }

    // Create "record" that points at the id of the resource
    Model record = ModelFactory.createDefaultModel();
    record.add(aModel);
    record.add(recordId, RDF.type, recordType);
    record.add(recordId, resourceLink, resource);

    // JSON-LD context
    Map<String, String> jsonLdContext = new HashMap<>();
    jsonLdContext.put("@context", aContextUrl);

    // JSON-LD frame
    Map<String, String> frame = new HashMap<>();
    frame.put("@context", aContextUrl);
    frame.put("@embed", "@always");
    frame.put("@type", recordType.toString());

    // Jena write context config
    JsonLdOptions jsonLdOptions = new JsonLdOptions();
    jsonLdOptions.setUseNativeTypes(true);
    jsonLdOptions.setOmitGraph(true);
    JsonLDWriteContext jenaWriteContext = new JsonLDWriteContext();
    jenaWriteContext.setJsonLDContext(jsonLdContext);
    jenaWriteContext.setFrame(frame);
    jenaWriteContext.setOptions(jsonLdOptions);

    // Serialize to JSON-LD and deserialize to JsonNode
    ByteArrayOutputStream boas = new ByteArrayOutputStream();
    DatasetGraph g = DatasetFactory.create(record).asDatasetGraph();
    mWriter.write(boas, g, RiotLib.prefixMap(g), null, jenaWriteContext);
    JsonNode jsonNode = mObjectMapper.readTree(boas.toByteArray());
    ObjectNode result = (ObjectNode) jsonNode.get(resourceLink.toString());
    result.put("@context", aContextUrl);

    return Resource.fromJson(prune(result));
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
      } else if (!value.isTextual() || !value.asText().startsWith("_:")) {
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
    List<Resource> resources = new ArrayList<>();
    String subjectsQuery = "SELECT DISTINCT ?s WHERE { ?s ?p ?o . FILTER isIRI(?s) }";

    RDFDataMgr
      .read(model, IOUtils.toInputStream(aResource.toString(), StandardCharsets.UTF_8), Lang.JSONLD);
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
