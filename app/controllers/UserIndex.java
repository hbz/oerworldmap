package controllers;

import play.mvc.*;
import play.data.*;
import services.ElasticsearchClient;
import models.User;

public class UserIndex extends Controller {

  private static ElasticsearchClient mClient = new ElasticsearchClient();

  public static Result get() {
    return ok(views.html.UserIndex.index.render(Form.form(User.class)));
  }

  public static Result post() {
    Form<User> requestData = Form.form(User.class).bindFromRequest();
    if (requestData.hasErrors()) {
      return badRequest(views.html.UserIndex.index.render(requestData));
    } else {
      User user = requestData.get();
      System.err.println(mClient.getClient().settings().getAsMap());
      mClient.getClient()
          .prepareIndex(ElasticsearchClient.getIndex(), ElasticsearchClient.getType())
          .setSource(user.toString())
          .execute().actionGet();
          
      return ok(views.html.UserIndex.registered.render(user.email));
    }
  }
  
}
