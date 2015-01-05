package controllers;

import java.util.ArrayList;
import java.util.List;

import play.mvc.*;
import play.data.Form;
import play.data.validation.ValidationError;
import models.User;

import org.apache.commons.validator.routines.EmailValidator;

public class UserIndex extends Controller {

    final static Form<User> registrationForm = Form.form(User.class);

    public static Result get() {
        return ok(views.html.UserIndex.index.render(registrationForm));
    }
    
    public static Result post() {
        String email = Form.form().bindFromRequest().get("email");
        List<ValidationError> errors = validate(email);
        if (errors != null && !errors.isEmpty()){
            return badRequest("E-mail error");				// TODO: to be specified
        }
        return ok(views.html.UserIndex.registered.render(email));
    }

    
    private static List<ValidationError> validate(String aEmail) {
	
	List<ValidationError> errors = new ArrayList<ValidationError>();
	
	if (User.byEmail(aEmail) != null) {
	    errors.add(new ValidationError("email", "This e-mail is already registered."));
	}
	else if(! EmailValidator.getInstance().isValid(aEmail)){
	    errors.add(new ValidationError("email", "This is not a valid e-mail adress."));
	}
	return errors.isEmpty() ? null : errors;
    }
}