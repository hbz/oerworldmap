package controllers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;

import helpers.Countries;
import helpers.JSONForm;
import helpers.JsonLdConstants;
import models.Resource;
import play.Logger;
import play.mvc.Result;
import play.mvc.Security;
import play.mvc.With;
import services.Account;

public class UserIndex extends OERWorldMap {

  public static Result list() throws IOException {
    Map<String, Object> scope = new HashMap<>();
    scope.put("countries", Countries.list(currentLocale));
    return ok(render("Registration", "UserIndex/index.mustache", scope));
  }

  public static Result create() throws IOException {

    Resource user = Resource.fromJson(JSONForm.parseFormData(request().body().asFormUrlEncoded()));
    Map<String, Object> scope = new HashMap<>();

    ProcessingReport report = user.validate();
    user.put("mbox_sha1sum", Account.getEncryptedEmailAddress(user));
    if (mConf.getBoolean("user.email.unique")) {
      ensureEmailUnique(user, report);
    }
    if (!report.isSuccess()) {
      scope.put("countries", Countries.list(currentLocale));
      scope.put("user", user);
      return badRequest(render("Registration", "UserIndex/index.mustache", scope,
          JSONForm.generateErrorReport(report)));
    }

    newsletterSignup(user);
    user.remove("email");
    mBaseRepository.addResource(user);

    List<Map<String, Object>> messages = new ArrayList<>();
    HashMap<String, Object> message = new HashMap<>();
    message.put("level", "success");
    message.put("message", UserIndex.messages.getString("user_registration_feedback"));
    messages.add(message);
    return ok(render("Registration", "feedback.mustache", scope, messages));

  }

  public static Result read(String id) throws IOException {
    return ResourceIndex.read(id);
  }

  public static Result update(String id) throws IOException {
    return ResourceIndex.update(id);
  }

  public static Result delete(String id) throws IOException {
    return ResourceIndex.delete(id);
  }

  public static Result sendToken() {

    Resource user = Resource.fromJson(JSONForm.parseFormData(request().body().asFormUrlEncoded()));
    ProcessingReport report = user.validate();

    Map<String, Object> scope = new HashMap<>();
    scope.put("user", user);

    if (!report.isSuccess()) {
      return badRequest(render("Request Token", "Secured/token.mustache", scope,
          JSONForm.generateErrorReport(report)));
    }

    String token = Account.createTokenFor(user);
    if (!StringUtils.isEmpty(token) && !StringUtils.isEmpty(mConf.getString("mail.smtp.host"))) {
      sendTokenMail(user, token);
    } else {
      Logger.info("No mailserver specified, cannot send ".concat(token).concat( " to "
        .concat(user.getAsString("email"))));
    }

    scope.put("continue", "<a class=\"hijax\" target=\"_self\" href=\"/.auth\">".concat(
      messages.getString("feedback_link_continue")).concat("</a>"));

    // We fail silently with success message in order not to expose valid email addresses
    List<Map<String, Object>> messages = new ArrayList<>();
    HashMap<String, Object> message = new HashMap<>();
    message.put("level", "success");
    message.put("message", UserIndex.messages.getString("user_token_request_description"));
    messages.add(message);

    return ok(render("Request Token", "feedback.mustache", scope, messages));

  }

  public static Result manageToken() {

    Map<String, Object> scope = new HashMap<>();
    scope.put("continue", "<a href=\"\">".concat(messages.getString("feedback_link_continue")).concat("</a>"));

    List<Map<String, Object>> messages = new ArrayList<>();
    HashMap<String, Object> message = new HashMap<>();
    message.put("level", "success");
    message.put("message", UserIndex.messages.getString("user_status_logged_in"));
    messages.add(message);

    return ok(render("Manage Token", "feedback.mustache", scope, messages));

  }

  public static Result deleteToken() {

    Map<String, Object> scope = new HashMap<>();
    scope.put("continue", "<a href=\"\">".concat(messages.getString("feedback_link_continue")).concat("</a>"));

    Resource user = new Resource("Person");
    user.put("email", request().username());
    Account.removeTokenFor(user);
    scope.put("user", user);

    List<Map<String, Object>> messages = new ArrayList<>();
    HashMap<String, Object> message = new HashMap<>();
    message.put("level", "success");
    message.put("message", UserIndex.messages.getString("user_status_logged_out"));
    messages.add(message);

    return ok(render("Delete Token", "feedback.mustache", scope, messages));

  }

  private static void ensureEmailUnique(Resource user, ProcessingReport aReport) {
    String aEmail = user.get("mbox_sha1sum").toString();
    String emailExistsQuery = JsonLdConstants.TYPE.concat(":Person").concat(" AND ")
        .concat("mbox_sha1sum:").concat(aEmail);
    if ((!(mBaseRepository.query(emailExistsQuery, 0, 1, null, null).getTotalItems() == 0))) {
      ProcessingMessage message = new ProcessingMessage();
      message.setMessage("This e-mail address is already registered");
      ObjectNode instance = new ObjectNode(JsonNodeFactory.instance);
      instance.put("pointer", "/mbox_sha1sum");
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
      Logger.warn("No mailman configured, user ".concat(user.get("email").toString())
        .concat(" not signed up for newsletter"));
      return;
    }

    @SuppressWarnings("resource")
    HttpClient client = new DefaultHttpClient();
    HttpPost request = new HttpPost("https://" + mailmanHost + "/mailman/subscribe/" + mailmanList);
    try {
      List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
      nameValuePairs.add(new BasicNameValuePair("email", user.get("email").toString()));
      request.setEntity(new UrlEncodedFormEntity(nameValuePairs));

      HttpResponse response = client.execute(request);
      Logger.info(Integer.toString(response.getStatusLine().getStatusCode()));
      BufferedReader rd = new BufferedReader(
          new InputStreamReader(response.getEntity().getContent()));
      String line = "";
      while ((line = rd.readLine()) != null) {
        Logger.info(line);
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void sendTokenMail(Resource aUser, String aToken) {
    Email confirmationMail = new SimpleEmail();
    try {
      confirmationMail.setMsg(UserIndex.messages.getString("user_token_request_message").concat("\n\n").concat(aToken));
      confirmationMail.setHostName(mConf.getString("mail.smtp.host"));
      confirmationMail.setSmtpPort(mConf.getInt("mail.smtp.port"));
      String smtpUser = mConf.getString("mail.smtp.user");
      String smtpPass = mConf.getString("mail.smtp.password");
      if (!smtpUser.isEmpty()) {
        confirmationMail.setAuthenticator(new DefaultAuthenticator(smtpUser, smtpPass));
      }
      confirmationMail.setSSLOnConnect(mConf.getBoolean("mail.smtp.ssl"));
      confirmationMail.setStartTLSEnabled(mConf.getBoolean("mail.smtp.tls"));
      confirmationMail.setFrom(mConf.getString("mail.smtp.from"),
        mConf.getString("mail.smtp.sender"));
      confirmationMail.setSubject(UserIndex.messages.getString("user_token_request_subject"));
      confirmationMail.addTo((String) aUser.get("email"));
      confirmationMail.send();
      Logger.debug(confirmationMail.toString());
      Logger.info("Sent " + aToken + " to " + aUser.get("email"));
    } catch (EmailException e) {
      Logger.debug(e.toString());
      Logger.error("Failed to send " + aToken + " to " + aUser.get("email"));
    }
  }

}
