package controllers;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
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

    ProcessingReport report = user.validate();
    if (mConf.getBoolean("user.email.unique")) {
      ensureEmailUnique(user.get("email").toString(), report);
    }
    if (!report.isSuccess()) {
      mResponseData.put("countries", Countries.list(currentLocale));
      mResponseData.put("errors", JSONForm.generateErrorReport(report));
      mResponseData.put("person", user);
      return badRequest(render("Registration", "UserIndex/index.mustache"));
    }

    user.put("confirmed", false);
    mUnconfirmedUserRepository.addResource(user);
    sendConfirmationMail(user);
    mResponseData.put("status", "success");
    mResponseData.put("message", i18n.get("user_registration_feedback") + " " + user.get("email"));
    return ok(render("Registration", "feedback.mustache"));

  }

  private static void ensureEmailUnique(String aEmail, ProcessingReport aReport) {
    // Actually, only checking the FileResourceRepository should suffice as resources remain there
    // after confirmation. I'll leave this is though, until File- and Elasticsearch sinks are wrapped
    // in a unifying OERWorldMapRepository.
    if ((!mUnconfirmedUserRepository.getResourcesByContent("Person", "email", aEmail).isEmpty())
        || (!mResourceRepository.getResourcesByContent("Person", "email", aEmail).isEmpty())) {
      ProcessingMessage message = new ProcessingMessage();
      message.setMessage("This e-mail address is already registered");
      ObjectNode instance = new ObjectNode(JsonNodeFactory.instance);
      instance.put("pointer", "/email");
      message.put("instance", instance);
      try {
        aReport.error(message);
      } catch (ProcessingException e) {
        e.printStackTrace();
      }
    }
  }

  public static Result confirm(String id) throws IOException {

    Resource user;

    try {
      // Ensure we don't needlessly reconfirm users
      if (!mResourceRepository.getResourcesByContent("Person", "@id", id).isEmpty()) {
        throw new IOException("User already confirmed");
      }
      user = mUnconfirmedUserRepository.getResource(id);
      user.remove("confirmed");
    } catch (IOException e) {
      e.printStackTrace();
      mResponseData.put("status", "warning");
      mResponseData.put("message", i18n.get("user_registration_confirmation_error"));
      mResponseData.put("continue", routes.LandingPage.get().absoluteURL(request()));
      return ok(render("Registration", "feedback.mustache"));
    }

    mUnconfirmedUserRepository.addResource(user);
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
      String smtpUser = mConf.getString("mail.smtp.user");
      String smtpPass = mConf.getString("mail.smtp.password");
      if (! smtpUser.isEmpty()) {
        confirmationMail.setAuthenticator(new DefaultAuthenticator(smtpUser, smtpPass));
      }
      confirmationMail.setSSLOnConnect(mConf.getBoolean("mail.smtp.ssl"));
      confirmationMail.setFrom(mConf.getString("mail.smtp.from"));
      confirmationMail.setSubject(i18n.get("user_registration_confirmation_mail_subject"));
      confirmationMail.addTo((String)user.get("email"));
      confirmationMail.send();
    } catch (EmailException e) {
      e.printStackTrace();
    }

  }

}
