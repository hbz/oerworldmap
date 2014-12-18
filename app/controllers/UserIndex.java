package controllers;

import play.*;
import play.mvc.*;
import play.data.*;
import play.libs.Json;
import models.User;

public class UserIndex extends Controller {

    final static Form<User> registrationForm = Form.form(User.class);

    public static Result get() {
        return ok(views.html.UserIndex.index.render(registrationForm));
    }

    public static Result post() {
        DynamicForm requestData = Form.form().bindFromRequest();
        String email = requestData.get("email");
        return ok(views.html.UserIndex.registered.render(email));
    }

}

