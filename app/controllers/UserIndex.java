package controllers;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import helpers.JSONForm;
import io.michaelallen.mustache.MustacheFactory;
import io.michaelallen.mustache.api.Mustache;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import models.Resource;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

import play.mvc.Result;
import helpers.Countries;

public class UserIndex extends OERWorldMap {

  public static Result get() throws IOException {
    mResponseData.put("countries", Countries.list(currentLocale));
    return ok(render("Registration", "UserIndex/index.mustache"));
  }

  public static Result post() throws IOException {

    Resource user = Resource.fromJson(JSONForm.parseFormData(request().body().asFormUrlEncoded()));

    // TODO: how to ensure uniqueness of email address?
    ProcessingReport report = user.validate();
    if (!report.isSuccess()) {
      mResponseData.put("countries", Countries.list(currentLocale));
      mResponseData.put("errors", JSONForm.generateErrorReport(report));
      mResponseData.put("person", user);
      return badRequest(render("Registration", "UserIndex/index.mustache"));
    }

    mUnconfirmedUserRepository.addResource(user);
    sendConfirmationMail(user);
    mResponseData.put("status", "success");
    mResponseData.put("message", i18n.get("user_registration_feedback") + " " + user.get("email"));
    return ok(render("Registration", "feedback.mustache"));

  }

  public static Result confirm(String id) throws IOException {

    Resource user;

    try {
      user = mUnconfirmedUserRepository.deleteResource(id);
    } catch (IOException e) {
      e.printStackTrace();
      mResponseData.put("status", "warning");
      mResponseData.put("message", i18n.get("user_registration_confirmation_error"));
      mResponseData.put("continue", routes.LandingPage.get().absoluteURL(request()));
      return ok(render("Registration", "feedback.mustache"));
    }

    mResourceRepository.addResource(user);
    mResponseData.put("status", "success");
    mResponseData.put("message", i18n.get("user_registration_confirmation_success"));
    mResponseData.put("continue", routes.LandingPage.get().absoluteURL(request()));
    return ok(render("Registration",  "feedback.mustache"));

  }

  private static void sendConfirmationMail(Resource user) {
    Email confirmationMail = new SimpleEmail();
    Map<String, Object> data = new HashMap<>();
    try {
      data.put("link", routes.UserIndex.post().absoluteURL(request()) + user.get("@id"));
      data.put("i18n", i18n);
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
      confirmationMail.setSubject(i18n.get("user_registration_confirmation_mail_subject"));
      confirmationMail.addTo((String)user.get("email"));
      confirmationMail.send();
    } catch (EmailException e) {
      e.printStackTrace();
    }

  }

}
