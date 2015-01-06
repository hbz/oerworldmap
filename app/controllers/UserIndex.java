package controllers;

import play.*;
import play.mvc.*;
import play.data.*;
import play.libs.Json;
import models.User;

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
            return ok(views.html.UserIndex.registered.render(user.email));
        }
    }

}

