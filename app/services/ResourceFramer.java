package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import helpers.JsonLdConstants;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.vocabulary.RDF;
import play.Logger;

/**
 * Created by fo on 23.03.16.
 */
public class ResourceFramer {

  private static final ObjectMapper mObjectMapper = new ObjectMapper();

  private static String mContextUrl;

  public static void setContext(String aContextUrl) {
    mContextUrl = aContextUrl;
  }

  public static void write(Model m) {
    DatasetGraph g = DatasetFactory.create(m).asDatasetGraph();
    WriterDatasetRIOT w = RDFDataMgr.createDatasetWriter(RDFFormat.JSONLD_FRAME_PRETTY);
    PrefixMap pm = RiotLib.prefixMap(g);
    JsonLDWriteContext context = new JsonLDWriteContext();
    context.setJsonLDContext("{\"@context\":\"https://oerworldmap.org/assets/json/context.json\"}");
    context.setFrame("{\"@context\":\"https://oerworldmap.org/assets/json/context.json\", \"@type\": \"http://schema.org/Action\", \"@embed\": \"@always\"}");
    w.write(System.out, g, pm, null, context) ;
  }

  public static Resource resourceFromModel(Model aModel, String aId) throws IOException {

    //write(aModel);

    NodeIterator types = aModel.listObjectsOfProperty(aModel.createResource(aId), RDF.type);

    if (types.hasNext()) {
      String type = types.next().toString();
      DatasetGraph g = DatasetFactory.create(aModel).asDatasetGraph();
      JsonLDWriteContext ctx = new JsonLDWriteContext();
      String context = String.format("{ \"@context\": \"%s\"}", mContextUrl);
      ctx.setJsonLDContext(context);
      ctx.setFrame("{\"@context\":\"https://oerworldmap.org/assets/json/context.json\", \"@embed\": \"@always\", \"@type\": \"" + type + "\"}");
      ByteArrayOutputStream boas = new ByteArrayOutputStream();
      WriterDatasetRIOT w = RDFDataMgr.createDatasetWriter(RDFFormat.JSONLD_FRAME_PRETTY);
      w.write(boas, g, RiotLib.prefixMap(g), null, ctx);
      JsonNode jsonNode = mObjectMapper.readTree(boas.toByteArray());
      if (jsonNode.has(JsonLdConstants.GRAPH)) {
        ArrayNode graphs = (ArrayNode) jsonNode.get(JsonLdConstants.GRAPH);
        for (JsonNode graph : graphs) {
          if (graph.get(JsonLdConstants.ID).asText().equals(aId)) {
            ObjectNode result = (ObjectNode) graph;
            result.put(JsonLdConstants.CONTEXT, mContextUrl);
            Logger.debug("Framed " + aId);
            return Resource.fromJson(result);
          }
        }
      } else {
        ObjectNode result = (ObjectNode) jsonNode;
        result.put(JsonLdConstants.CONTEXT, mContextUrl);
        Logger.debug("Framed " + aId);
        return Resource.fromJson(result);
      }
    }

    return null;
  }

  public static List<Resource> flatten(Resource resource) throws IOException {

    Model model = ModelFactory.createDefaultModel();
    RDFDataMgr
      .read(model, IOUtils.toInputStream(resource.toString(), StandardCharsets.UTF_8), Lang.JSONLD);
    List<Resource> resources = new ArrayList<>();

    String subjectsQuery = "SELECT DISTINCT ?s WHERE { ?s ?p ?o . FILTER isIRI(?s) }";
    try (QueryExecution queryExecution = QueryExecutionFactory
      .create(QueryFactory.create(subjectsQuery), model)) {
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
