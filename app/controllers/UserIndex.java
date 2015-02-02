package controllers;

import models.Resource;
import models.User;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.search.aggregations.AggregationBuilder;

import play.data.DynamicForm;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import services.*;

import org.elasticsearch.node.Node;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.aggregations.AggregationBuilders;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

public class UserIndex extends Controller {

  private static Settings clientSettings = ImmutableSettings.settingsBuilder()
      .put(new ElasticsearchConfig().getClientSettings()).build();
  private static Client mClient = new TransportClient(clientSettings)
      .addTransportAddress(new InetSocketTransportAddress(new ElasticsearchConfig().getServer(),
          9300));
  private static ElasticsearchClient mElasticsearchClient = new ElasticsearchClient(mClient);
  private static ElasticsearchRepository resourceRepository = new ElasticsearchRepository(
      mElasticsearchClient);

  public static Result get() throws IOException {
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
      if (!"".equals(countryCode)) {
        Resource address = new Resource("PostalAddress");
        address.put("countryName", requestData.get("address.addressCountry"));
        user.put("address", address);
      }
      resourceRepository.addResource(user);
      return ok(views.html.UserIndex.registered.render((String) user.get("email")));
    }
  }

}
