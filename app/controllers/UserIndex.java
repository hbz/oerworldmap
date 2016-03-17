package controllers;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
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
import services.AccountService;

public class UserIndex extends OERWorldMap {

  private static AccountService accountService = new AccountService(
    new File(Global.getConfig().getString("user.token.dir")), new File(Global.getConfig().getString("ht.passwd")));

  public static Result signup() {

    Map<String, Object> scope = new HashMap<>();
    scope.put("countries", Countries.list(Locale.getDefault()));
    return ok(render("Registration", "UserIndex/register.mustache", scope));

  }

  public static Result register() {

    Resource user = Resource.fromJson(JSONForm.parseFormData(request().body().asFormUrlEncoded()));

    String username = user.getAsString("email");
    String password = user.getAsString("password");
    String confirm = user.getAsString("password-confirm");
    user.remove("password");
    user.remove("password-confirm");

    Result result;

    if (StringUtils.isEmpty(username)) {
      result = badRequest("No Username provided.");
    } else if (StringUtils.isEmpty(password)) {
      result = badRequest("No Password provided.");
    } else if (!password.equals(confirm)) {
      result = badRequest("Passwords must match.");
    } else {
      String token = accountService.addUser(username, password);
      if (token == null) {
        result = badRequest("Failed to add ".concat(username));
      } else {
        sendMail(username, "Token: ".concat(token));
        result = ok("Added ".concat(username));
      }
    }

    return result;

  }

  public static Result verify(String token) {

    Result result;

    if (token == null) {
      result = badRequest("No token given.");
    } else {
      String username = accountService.verifyToken(token);
      if (username != null) {
        result = ok("Verified ".concat(username));
      } else {
        result = badRequest("Invalid token ".concat(token));
      }
    }

    return result;

  }

  public static Result create() throws IOException {
    boolean isJsonRequest = true;
    JsonNode json = request().body().asJson();
    if (null == json) {
      Map<String, String[]> formUrlEncoded = request().body().asFormUrlEncoded();
      if (null == formUrlEncoded) {
        return badRequest("Empty request body");
      }
      json = JSONForm.parseFormData(formUrlEncoded, true);
      isJsonRequest = false;
    }
    Resource resource = Resource.fromJson(json);
    String id = resource.getAsString(JsonLdConstants.ID);
    ProcessingReport report = mBaseRepository.validateAndAdd(resource);
    Map<String, Object> scope = new HashMap<>();
    scope.put("resource", resource);
    if (!report.isSuccess()) {
      scope.put("countries", Countries.list(Locale.getDefault()));
      if (isJsonRequest) {
        return badRequest("Failed to create " + id + "\n" + report.toString() + "\n");
      } else {
        List<Map<String, Object>> messages = new ArrayList<>();
        HashMap<String, Object> message = new HashMap<>();
        message.put("level", "warning");
        message.put("message", OERWorldMap.messages.getString("schema_error")  + "<pre>" + report.toString() + "</pre>"
          + "<pre>" + resource + "</pre>");
        messages.add(message);
        return badRequest(render("Create failed", "feedback.mustache", scope, messages));
      }
    }
    response().setHeader(LOCATION, routes.ResourceIndex.create().absoluteURL(request()).concat(id));
    if (isJsonRequest) {
      return created("Created " + id + "\n");
    } else {
      return created(render("Created", "created.mustache", scope));
    }
  }

  public static Result read(String id) throws IOException {
    return ResourceIndex.read(id);
  }

  public static Result update(String id) throws IOException {
    Resource originalResource = mBaseRepository.getResource(id);
    if (originalResource == null) {
      return badRequest("missing resource " + id);
    }

    boolean isJsonRequest = true;
    JsonNode json = request().body().asJson();
    if (null == json) {
      json = JSONForm.parseFormData(request().body().asFormUrlEncoded(), true);
      isJsonRequest = false;
    }
    Resource resource = Resource.fromJson(json);
    ProcessingReport report = mBaseRepository.validateAndAdd(resource);
    Map<String, Object> scope = new HashMap<>();
    scope.put("resource", resource);
    if (!report.isSuccess()) {
      scope.put("countries", Countries.list(Locale.getDefault()));
      if (isJsonRequest) {
        return badRequest("Failed to update " + id + "\n" + report.toString() + "\n");
      } else {
        List<Map<String, Object>> messages = new ArrayList<>();
        HashMap<String, Object> message = new HashMap<>();
        message.put("level", "warning");
        message.put("message", OERWorldMap.messages.getString("schema_error")  + "<pre>" + report.toString() + "</pre>"
          + "<pre>" + resource + "</pre>");
        messages.add(message);
        return badRequest(render("Update failed", "feedback.mustache", scope, messages));
      }
    }
    if (isJsonRequest) {
      return ok("Updated " + id + "\n");
    } else {
      return ok(render("Updated", "updated.mustache", scope));
    }
  }

  public static Result delete(String id) throws IOException {
    return ResourceIndex.delete(id);
  }

  public static Result requestPassword() {
    return ok(render("Reset Password", "UserIndex/password.mustache"));
  }

  public static Result sendPassword() {

    Result result;

    Resource user = Resource.fromJson(JSONForm.parseFormData(request().body().asFormUrlEncoded()));

    String username;
    if (ctx().args.get("username") != null) {
      username = ctx().args.get("username").toString();
      String password = user.getAsString("password");
      String updated = user.getAsString("password-new");
      String confirm = user.getAsString("password-confirm");
      if (StringUtils.isEmpty(password) || StringUtils.isEmpty(updated) || StringUtils.isEmpty(confirm)) {
        result = badRequest("Please fill out the form.");
      } else if (!updated.equals(confirm)) {
        result = badRequest("Passwords must match.");
      } else if (!accountService.updatePassword(username, password, updated)) {
        result = badRequest("Failed to update password for ".concat(username));
      } else {
        result = ok("Password changed.");
      }
    } else {
      username = user.getAsString("email");
      if (StringUtils.isEmpty(username) || !accountService.userExists(username)) {
        result = badRequest("No valid username provided.");
      } else {
        String password = new BigInteger(130, new SecureRandom()).toString(32);
        if (accountService.setPassword(username, password)) {
          sendMail(username, password);
          result = ok("Password successfully reset.");
        } else {
          result = badRequest("Failed to reset password.");
        }
      }
    }

    return result;

  }

  public static Result newsletterSignup() {

    Map<String, Object> scope = new HashMap<>();
    scope.put("countries", Countries.list(Locale.getDefault()));
    return ok(render("Registration", "UserIndex/newsletter.mustache", scope));

  }

  public static Result newsletterRegister() {

    Resource user = Resource.fromJson(JSONForm.parseFormData(request().body().asFormUrlEncoded()));

    if (!user.validate().isSuccess()) {
      return badRequest("Please provide a valid email address and select a country.");
    }

    String username = user.getAsString("email");

    if (StringUtils.isEmpty(username)) {
      return badRequest("Not username given.");
    }

    String mailmanHost = mConf.getString("mailman.host");
    String mailmanList = mConf.getString("mailman.list");
    if (mailmanHost.isEmpty() || mailmanList.isEmpty()) {
      Logger.warn("No mailman configured, user ".concat(username)
        .concat(" not signed up for newsletter"));
      return internalServerError("Newletter currently not available.");
    }

    @SuppressWarnings("resource")
    HttpClient client = new DefaultHttpClient();
    HttpPost request = new HttpPost("https://" + mailmanHost + "/mailman/subscribe/" + mailmanList);
    try {
      List<NameValuePair> nameValuePairs = new ArrayList<>(1);
      nameValuePairs.add(new BasicNameValuePair("email", user.get("email").toString()));
      request.setEntity(new UrlEncodedFormEntity(nameValuePairs));
      HttpResponse response = client.execute(request);
      Integer responseCode = response.getStatusLine().getStatusCode();

      if (!responseCode.equals(200)) {
        Logger.error(response.getStatusLine().toString());
        return internalServerError();
      }

    } catch (IOException e) {
      Logger.error(e.toString());
      return internalServerError();
    }

    return ok(username + " signed up for newsletter");

  }

  private static void sendMail(String aEmailAddress, String aMessage) {
    Email mail = new SimpleEmail();
    try {
      mail.setMsg(aMessage);
      mail.setHostName(mConf.getString("mail.smtp.host"));
      mail.setSmtpPort(mConf.getInt("mail.smtp.port"));
      String smtpUser = mConf.getString("mail.smtp.user");
      String smtpPass = mConf.getString("mail.smtp.password");
      if (!smtpUser.isEmpty()) {
        mail.setAuthenticator(new DefaultAuthenticator(smtpUser, smtpPass));
      }
      mail.setSSLOnConnect(mConf.getBoolean("mail.smtp.ssl"));
      mail.setStartTLSEnabled(mConf.getBoolean("mail.smtp.tls"));
      mail.setFrom(mConf.getString("mail.smtp.from"),
        mConf.getString("mail.smtp.sender"));
      mail.setSubject(UserIndex.messages.getString("user_token_request_subject"));
      mail.addTo(aEmailAddress);
      mail.send();
      Logger.debug(mail.toString());
      Logger.info("Sent\n" + aMessage + "\nto " + aEmailAddress);
    } catch (EmailException e) {
      Logger.error(e.toString());
      Logger.debug("Failed to send\n" + aMessage + "\nto " + aEmailAddress);
    }
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

}
