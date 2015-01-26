package controllers;

import models.User;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import services.ElasticsearchClient;

public class UserIndex extends Controller {

  public static Result get() {
    return ok(views.html.UserIndex.index.render(Form.form(User.class)));
  }

  public static Result post() {
    Form<User> requestData = Form.form(User.class).bindFromRequest();
    if (requestData.hasErrors()) {
      return badRequest(views.html.UserIndex.index.render(requestData));
    } else {
      User user = requestData.get();
      System.err.println(ElasticsearchClient.getClient().settings().getAsMap());
      ElasticsearchClient.getClient()
          .prepareIndex(ElasticsearchClient.getIndex(), ElasticsearchClient.getType())
          .setSource(user.toString())
          .execute().actionGet();
          
      return ok(views.html.UserIndex.registered.render(user.email));
    }
  }
  
}
