package controllers;

import play.*;
import play.mvc.*;

public class LandingPage extends Controller {

    public static Result get() {
        return ok(views.html.LandingPage.index.render());
    }

}
