package controllers;

import models.Resource;
import models.User;
import play.data.DynamicForm;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import services.ElasticsearchClient;
import services.ElasticsearchConfig;
import services.ElasticsearchRepository;
import services.ResourceRepository;
import org.elasticsearch.node.Node;
import org.elasticsearch.client.Client;


import java.io.IOException;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

public class UserIndex extends Controller {

  public static Result get() {
    return ok(views.html.UserIndex.index.render());
  }

  public static Result post() throws IOException {
    DynamicForm requestData = Form.form().bindFromRequest();
    if (requestData.hasErrors()) {
      return badRequest(views.html.UserIndex.index.render());
    } else {
      Resource user = new Resource("Person");
      user.put("email", requestData.get("email"));
      String countryCode = requestData.get("address.addressCountry");
      if (! "".equals(countryCode)) {
        Resource address = new Resource("PostalAddress");
        address.put("countryName", requestData.get("address.addressCountry"));
        user.put("address", address);
      }
      ElasticsearchConfig esConfig = new ElasticsearchConfig();
      Node mNode = nodeBuilder().settings(esConfig.getClientSettings()).node();
      Client mClient = mNode.client();
      ElasticsearchClient mElasticsearchClient = new ElasticsearchClient(mClient);
      ResourceRepository resourceRepository = new ElasticsearchRepository(mElasticsearchClient);
      resourceRepository.addResource(user);
      return ok(views.html.UserIndex.registered.render((String) user.get("email")));
    }
  }
  
}
