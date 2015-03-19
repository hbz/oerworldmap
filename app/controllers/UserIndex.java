package controllers;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import helpers.JSONForm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import models.Resource;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import play.mvc.Result;
import helpers.Countries;
import play.mvc.Security;
import services.Account;

public class UserIndex extends OERWorldMap {

  private static final String REALM = "Basic realm=\"OER World Map\"";

  private static List<String> loggedOutUsers = new ArrayList<>();

  public static Result get() throws IOException {
    mResponseData.put("countries", Countries.list(currentLocale));
    return ok(render("Registration", "UserIndex/index.mustache"));
  }

  public static Result post() throws IOException {

    Resource user = Resource.fromJson(JSONForm.parseFormData(request().body().asFormUrlEncoded()));

    ProcessingReport report = user.validate();
    if (mConf.getBoolean("user.email.unique")) {
      ensureEmailUnique(user, report);
    }
    if (!report.isSuccess()) {
      mResponseData.put("countries", Countries.list(currentLocale));
      mResponseData.put("errors", JSONForm.generateErrorReport(report));
      mResponseData.put("person", user);
      return badRequest(render("Registration", "UserIndex/index.mustache"));
    }

    // newsletterSignup(user);
    user.put("email", Account.getEncryptedEmailAddress(user));
    mBaseRepository.addResource(user);

    mResponseData.put("status", "success");
    mResponseData.put("message", i18n.get("user_registration_feedback"));
    return ok(render("Registration", "feedback.mustache"));

  }

  @Security.Authenticated(Secured.class)
  public static Result auth() {
    String form = "<form action=\"/user/.logout\" method=\"post\"><input type=\"submit\" value=\"logout\" /></form>";
    return ok("Welcome to protected, " + request().username() + form).as("text/html");
  }

  public static Result requestToken() {
    return ok(render("Request Token", "UserIndex/token.mustache"));
  }

  public static Result sendToken() {
    Resource user = Resource.fromJson(JSONForm.parseFormData(request().body().asFormUrlEncoded()));
    ProcessingReport report = user.validate();
    if (!report.isSuccess()) {
      mResponseData.put("errors", JSONForm.generateErrorReport(report));
      return badRequest(render("Request Token", "UserIndex/token.mustache"));
    }
    String token = Account.createTokenFor(user);
    sendTokenMail(user, token);
    mResponseData.put("status", "success");
    mResponseData.put("message", i18n.get("user_token_request_feedback"));
    return ok(render("Request Token", "feedback.mustache"));
  }

  @Security.Authenticated(Secured.class)
  public static Result deleteToken() {
    Resource user = new Resource("Person");
    user.put("email", request().username());
    Account.removeTokenFor(user);
    mResponseData.put("status", "success");
    mResponseData.put("message", i18n.get("user_token_delete_feedback"));
    return ok(render("Delete Token", "feedback.mustache"));
  }

  private static void ensureEmailUnique(Resource user, ProcessingReport aReport) {
    String aEmail = Account.getEncryptedEmailAddress(user);
    // Actually, only checking the FileResourceRepository should suffice as
    // resources remain there
    // after confirmation. I'll leave this is though, until File- and
    // Elasticsearch sinks are wrapped
    // in a unifying OERWorldMapRepository.
    if ((!mBaseRepository.getResourcesByContent("Person", "email", aEmail, true).isEmpty())) {
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

  private static void newsletterSignup(Resource user) {

    String mailmanHost = mConf.getString("mailman.host");
    String mailmanList = mConf.getString("mailman.list");
    if (mailmanHost.isEmpty() || mailmanList.isEmpty()) {
      return;
    }

    HttpClient client = new DefaultHttpClient();
    HttpPost request = new HttpPost("https://" + mailmanHost + "/mailman/subscribe/" + mailmanList);
    try {
      List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
      nameValuePairs.add(new BasicNameValuePair("email", user.get("email").toString()));
      request.setEntity(new UrlEncodedFormEntity(nameValuePairs));

      HttpResponse response = client.execute(request);
      System.out.println(response.getStatusLine().getStatusCode());
      BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity()
          .getContent()));
      String line = "";
      while ((line = rd.readLine()) != null) {
        System.out.println(line);
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void sendTokenMail(Resource aUser, String aToken) {
    Email confirmationMail = new SimpleEmail();
    try {
      confirmationMail.setMsg("Your new token is " + aToken);
      confirmationMail.setHostName(mConf.getString("mail.smtp.host"));
      confirmationMail.setSmtpPort(mConf.getInt("mail.smtp.port"));
      String smtpUser = mConf.getString("mail.smtp.user");
      String smtpPass = mConf.getString("mail.smtp.password");
      if (!smtpUser.isEmpty()) {
        confirmationMail.setAuthenticator(new DefaultAuthenticator(smtpUser, smtpPass));
      }
      confirmationMail.setSSLOnConnect(mConf.getBoolean("mail.smtp.ssl"));
      confirmationMail.setFrom(mConf.getString("mail.smtp.from"),
          mConf.getString("mail.smtp.sender"));
      confirmationMail.setSubject(i18n.get("user_token_request_subject"));
      confirmationMail.addTo((String) aUser.get("email"));
      confirmationMail.send();
      System.out.println(confirmationMail.toString());
    } catch (EmailException e) {
      e.printStackTrace();
      System.out.println("Failed to send " + aToken + " to " + aUser.get("email"));
    }
  }

}
