package services;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import com.fasterxml.jackson.databind.JsonNode;

import io.apigee.trireme.core.NodeEnvironment;
import io.apigee.trireme.core.NodeException;
import io.apigee.trireme.core.NodeScript;
import io.apigee.trireme.core.ScriptFuture;
import models.Resource;
import org.apache.jena.shared.Lock;
import org.apache.jena.vocabulary.RDF;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;
import play.Logger;
import services.repository.TriplestoreRepository;

/**
 * Created by fo on 23.03.16.
 */
public class ResourceFramer {

  private static final AsyncHttpClient mAsyncHttpClient = new DefaultAsyncHttpClient();

  private static int mPort = 8080;

  private static NodeEnvironment mEnv = new NodeEnvironment();

  private static ScriptFuture mFramer = null;

  public static void start() {

    try {
      NodeScript script = mEnv.createScript("frame.js", new File("node/json-frame/frame.js"),
        new String[]{Integer.toString(mPort)});
      script.setNodeVersion("0.10");
      mFramer = script.executeModule();
      mFramer.getModuleResult();
    } catch (InterruptedException | ExecutionException | NodeException e) {
      Logger.error(e.toString());
    }

  }

  public static void stop() {

    if (mFramer != null) {
      mFramer.cancel(true);
    }

  }

  public static void setPort(int aPort) {
    mPort = aPort;
  }

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
        RDFDataMgr.write(unframed, dbstate, Lang.JSONLD);
        unframed.close();

        NodeIterator types = aModel.listObjectsOfProperty(aModel.createResource(aId), RDF.type);

        if (types.hasNext()) {
          String id = URLEncoder.encode(aId, StandardCharsets.UTF_8.toString());
          String type = URLEncoder.encode(types.next().toString(), StandardCharsets.UTF_8.toString());
          String url = "http://localhost:".concat(Integer.toString(mPort)).concat("/").concat(type).concat("/").concat(id);
          Future<Response> f = mAsyncHttpClient.preparePost(url).setBody(unframed.toByteArray()).execute();

          try {
            return Resource.fromJson(f.get().getResponseBody());
          } catch (InterruptedException | ExecutionException e) {
            Logger.error("Could not create resource from model", e);
          }

        }
      }
    } finally {
      dbstate.leaveCriticalSection();
      aModel.leaveCriticalSection();
    }

    return null;

  }

  public static List<Resource> flatten(Resource resource) throws IOException {

    String url = "http://localhost:".concat(Integer.toString(mPort)).concat("/flatten/");
    String body = resource.toJson().toString();
    Future<Response> f = mAsyncHttpClient.preparePost(url).setBody(body).execute();

    try {
      JsonNode results =  new ObjectMapper().readTree(f.get().getResponseBody());
      List<Resource> resources = new ArrayList<>();
      for (JsonNode result : results) {
        resources.add(Resource.fromJson(result));
      }
      return resources;
    } catch (InterruptedException | ExecutionException e) {
      Logger.error("Could not flatten resource", e);
    }

    return null;

  }

}
