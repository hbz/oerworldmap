package services;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import com.fasterxml.jackson.databind.JsonNode;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.vocabulary.RDF;
import com.ning.http.client.AsyncHttpClientConfig;

import io.apigee.trireme.core.NodeEnvironment;
import io.apigee.trireme.core.NodeException;
import io.apigee.trireme.core.NodeScript;
import io.apigee.trireme.core.ScriptFuture;
import models.Resource;
import play.Logger;
import play.libs.F;
import play.libs.ws.WSResponse;
import play.libs.ws.ning.NingWSClient;
import services.repository.TriplestoreRepository;

/**
 * Created by fo on 23.03.16.
 */
public class ResourceFramer {

  private static final NingWSClient mWSClient = new NingWSClient(new AsyncHttpClientConfig.Builder().build());

  static {
    NodeEnvironment env = new NodeEnvironment();
    try {
      NodeScript script = env.createScript("frame.js", new File("node/json-frame/frame.js"), null);
      script.setNodeVersion("0.10");
      ScriptFuture framer = script.executeModule();
      framer.getModuleResult();
    } catch (InterruptedException | ExecutionException | NodeException e) {
      Logger.error(e.toString());
    }
  }

  public static Resource resourceFromModel(Model aModel, String aId) throws IOException {

    String describeStatement = String.format(TriplestoreRepository.DESCRIBE_RESOURCE, aId);
    Model dbstate = ModelFactory.createDefaultModel();

    aModel.enterCriticalSection(Lock.READ);
    dbstate.enterCriticalSection(Lock.WRITE);
    try {
      try (QueryExecution queryExecution = QueryExecutionFactory.create(QueryFactory.create(describeStatement),
          aModel)) {
        queryExecution.execDescribe(dbstate);

        ByteArrayOutputStream unframed = new ByteArrayOutputStream();
        RDFDataMgr.write(unframed, dbstate, Lang.JSONLD);
        unframed.close();

        NodeIterator types = aModel.listObjectsOfProperty(aModel.createResource(aId), RDF.type);

        if (types.hasNext()) {
          String id = URLEncoder.encode(aId, StandardCharsets.UTF_8.toString());
          String type = URLEncoder.encode(types.next().toString(), StandardCharsets.UTF_8.toString());
          F.Promise<JsonNode> promise = mWSClient.url("http://localhost:8080/".concat(type).concat("/").concat(id))
              .post(new String(unframed.toByteArray(), StandardCharsets.UTF_8)).map(WSResponse::asJson);

          return Resource.fromJson(promise.get(Integer.MAX_VALUE));
        }
      }
    } finally {
      dbstate.leaveCriticalSection();
      aModel.leaveCriticalSection();
    }

    return null;

  }

}
