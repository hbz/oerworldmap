package controllers;

import io.michaelallen.mustache.MustacheFactory;
import io.michaelallen.mustache.api.Mustache;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.apache.commons.validator.routines.EmailValidator;

import play.data.DynamicForm;
import play.data.Form;
import play.data.validation.ValidationError;
import play.mvc.Result;

import helpers.Countries;

public class UserIndex extends OERWorldMap {

  public static Result get() throws IOException {
    Map<String, Object> data = new HashMap<>();
    data.put("countries", Countries.list(currentLocale));
    return ok(render("Registration", data, "UserIndex/index.mustache"));
  }

  public static Result post() throws IOException {

    Map<String, Object> data = new HashMap<>();
    DynamicForm requestData = Form.form().bindFromRequest();

    if (requestData.hasErrors()) {

      data.put("countries", Countries.list(currentLocale));
      return badRequest(render("Registration", data, "UserIndex/index.mustache"));

    } else {

      // Store user data
      Resource user = new Resource("Person");
      String email = requestData.get("email");
      String countryCode = requestData.get("address.addressCountry");

      List<ValidationError> validationErrors = checkEmailAddress(email);
      validationErrors.addAll(checkCountryCode(countryCode));

      if (!validationErrors.isEmpty()) {
        data.put("status", "warning");
        data.put("message", errorsToHtml(validationErrors));
        return badRequest(render("Registration", data, "feedback.mustache"));
      } else {
        user.put("email", email);

        if (!"".equals(countryCode)) {
          Resource address = new Resource("PostalAddress");
          address.put("countryName", countryCode);
          user.put("address", address);
        }
        mUnconfirmedUserRepository.addResource(user);

        // Send confirmation mail
        Email confirmationMail = new SimpleEmail();

        try {
          data.put("link", routes.UserIndex.post().absoluteURL(request()) + user.get("@id"));
          Mustache template = MustacheFactory.compile("UserIndex/confirmation.mustache");
          Writer writer = new StringWriter();
          template.execute(writer, data);
          confirmationMail.setMsg(writer.toString());
          confirmationMail.setHostName(mConf.getString("mail.smtp.host"));
          confirmationMail.setSmtpPort(mConf.getInt("mail.smtp.port"));
          confirmationMail.setAuthenticator(
                  new DefaultAuthenticator(mConf.getString("mail.smtp.user"), mConf.getString("mail.smtp.password"))
          );
          confirmationMail.setSSLOnConnect(true);
          confirmationMail.setFrom("oerworldmap@gmail.com");
          confirmationMail.setSubject("Please confirm");
          confirmationMail.addTo((String)user.get("email"));
          confirmationMail.send();
        } catch (EmailException e) {
          e.printStackTrace();
        }

        data.put("status", "success");
        data.put("message", "Thank you for your interest in the OER World Map. Your email address <em>"
                + user.get("email") + "</em> has been registered."
        );
        return ok(render("Registration", data, "feedback.mustache"));
      }
    }

  }

  private static List<ValidationError> checkEmailAddress(String aEmail) {

    List<ValidationError> errors = new ArrayList<ValidationError>();
    if (StringUtils.isEmpty(aEmail)){
      errors.add(new ValidationError("email", "Please specify an email address."));
    }
    else if (!resourceRepository.getResourcesByContent("Person", "email", aEmail).isEmpty()) {
      errors.add(new ValidationError("email", "This e-mail is already registered."));
    } else if (!EmailValidator.getInstance().isValid(aEmail)) {
      errors.add(new ValidationError("email", "This is not a valid e-mail adress."));
    }
    return errors;
  }

  private static List<ValidationError> checkCountryCode(String aCountryCode) {

    List<ValidationError> errors = new ArrayList<ValidationError>();

    List<String> validCodes = new ArrayList<>();

    for (Map<String, String> country : Countries.list(currentLocale)) {
      validCodes.add(country.get("alpha-2"));
    }

    if (StringUtils.isEmpty(aCountryCode)){
      errors.add(new ValidationError("countryName", "Please specify a country code."));
    }
    else if (!validCodes.contains(aCountryCode.toUpperCase())) {
      errors.add(new ValidationError("countryName", "This country is not valid."));
    }
    return errors;
  }

  private static String errorsToHtml(List<ValidationError> aErrorList) {
    String html = "<ul>";
    for (ValidationError error : aErrorList) {
      html = html.concat("<li>".concat(error.message()).concat("</li>"));
    }
    return html.concat("</ul>");
  }


  public static Result confirm(String id) throws IOException {

    Resource user;
    Map<String,Object> data = new HashMap<>();

    try {
      user = mUnconfirmedUserRepository.deleteResource(id);
    } catch (IOException e) {
      e.printStackTrace();
      data.put("status", "warning");
      data.put("message", "Error confirming email address");
      return ok(render("Registration", data, "feedback.mustache"));
    }

    resourceRepository.addResource(user);
    data.put("status", "success");
    data.put("message", "Thank you for your interest in the OER World Map. Your email address <em>"
            + user.get("email") + "</em> has been confirmed."
    );
    return ok(render("Registration", data, "feedback.mustache"));

  }

}
