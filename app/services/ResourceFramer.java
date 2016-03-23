package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.ning.http.client.AsyncHttpClientConfig;
import io.apigee.trireme.core.NodeEnvironment;
import io.apigee.trireme.core.NodeException;
import io.apigee.trireme.core.NodeScript;
import io.apigee.trireme.core.ScriptFuture;
import models.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import play.Logger;
import play.libs.F;
import play.libs.ws.WSResponse;
import play.libs.ws.ning.NingWSClient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

/**
 * Created by fo on 23.03.16.
 */
public class ResourceFramer {

  static {
    NodeEnvironment env = new NodeEnvironment();
    try {
      NodeScript script = env.createScript("frame.js",
        new File("node/json-frame/frame.js"), null);
      script.setNodeVersion("0.10");
      ScriptFuture framer = script.executeModule();
      framer.getModuleResult();
    } catch (InterruptedException | ExecutionException | NodeException e) {
      Logger.error(e.toString());
    }
  }

  public static Resource resourceFromModel(Model model, String id) throws IOException {

    Logger.debug("------------------model---------------------");
    model.write(System.out, "TURTLE");

    ByteArrayOutputStream nquads = new ByteArrayOutputStream();
    RDFDataMgr.write(nquads, model, Lang.NQUADS);

    AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();
    NingWSClient wsClient = new NingWSClient(builder.build());

    NodeIterator types = model.listObjectsOfProperty(model.createResource(id), RDF.type);

    if (types.hasNext()) {
      String type = URLEncoder.encode(
        types.next().toString(),
        StandardCharsets.UTF_8.toString());
      F.Promise<JsonNode> promise = wsClient.url("http://localhost:8080/".concat(type).concat("/").concat(id))
        .setContentType("text/plain").post(new String(nquads.toByteArray(), StandardCharsets.UTF_8))
        .map(WSResponse::asJson);

      return Resource.fromJson(promise.get(1000));
    }

    return null;

  }

}
