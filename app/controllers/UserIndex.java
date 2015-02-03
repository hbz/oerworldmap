package controllers;

import models.Resource;
import models.User;

import org.apache.commons.validator.routines.EmailValidator;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.search.aggregations.AggregationBuilder;

import play.data.DynamicForm;
import play.data.Form;
import play.data.validation.ValidationError;
import play.mvc.Controller;
import play.mvc.Result;
import services.*;

import org.elasticsearch.node.Node;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.aggregations.AggregationBuilders;

import helpers.UniversalFunctions;

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
      String email = requestData.get("email");

      List<ValidationError> validationErrors = checkEmailAddress(email);
      if (validationErrors != null && !validationErrors.isEmpty()) {
        return badRequest("E-mail error: ".concat(
            UniversalFunctions.collectionToString(validationErrors)));
      } else {
        user.put("email", email);
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

  private static List<ValidationError> checkEmailAddress(String aEmail) {

    List<ValidationError> errors = new ArrayList<ValidationError>();

    if (!resourceRepository.getResourcesByContent("Person", "email", aEmail).isEmpty()) {
      errors.add(new ValidationError("email", "This e-mail is already registered."));
    } else if (!EmailValidator.getInstance().isValid(aEmail)) {
      errors.add(new ValidationError("email", "This is not a valid e-mail adress."));
    }
    return errors.isEmpty() ? null : errors;
  }

}
