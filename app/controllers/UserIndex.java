package controllers;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import helpers.JsonLdConstants;
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

import helpers.Countries;
import helpers.JSONForm;
import models.Resource;
import play.Logger;
import play.mvc.Result;

public class UserIndex extends OERWorldMap {

  public static Result signup() {

    Map<String, Object> scope = new HashMap<>();
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
      result = badRequest("No email address provided.");
    } else if (StringUtils.isEmpty(password)) {
      result = badRequest("No password provided.");
    } else if (!password.equals(confirm)) {
      result = badRequest("Passwords must match.");
    } else if (password.length() < 8) {
      result = badRequest("Password must be at least 8 characters long.");
    } else {
      String token = mAccountService.addUser(username, password);
      if (token == null) {
        result = badRequest("Failed to add " . concat(username));
      } else {
        sendMail(username, MessageFormat.format(emails.getString("account.verify.message"),
            mConf.getString("proxy.host").concat(routes.UserIndex.verify(token).url())),
            emails.getString("account.verify.subject"));
        Map<String, Object> scope = new HashMap<>();
        scope.put("username", username);
        result = ok(render("Successfully registered", "UserIndex/registered.mustache", scope));
      }
    }

    return result;

  }

  public static Result verify(String token) throws IOException {

    Result result;

    if (token == null) {
      result = badRequest("No token given.");
    } else {
      String username = mAccountService.verifyToken(token);
      if (username != null) {

        createProfile(username);
        Map<String, Object> scope = new HashMap<>();
        scope.put("username", username);
        result = ok(render("User verified", "UserIndex/verified.mustache", scope));

      } else {
        result = badRequest("Invalid token ".concat(token));
      }
    }

    return result;

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
      } else if (password.length() < 8) {
        result = badRequest("Password must be at least 8 characters long.");
      } else if (!mAccountService.updatePassword(username, password, updated)) {
        result = badRequest("Failed to update password for ".concat(username));
      } else {
        result = ok(render("Password changed", "UserIndex/passwordChanged.mustache"));
      }
    } else {
      username = user.getAsString("email");
      if (StringUtils.isEmpty(username) || !mAccountService.userExists(username)) {
        result = badRequest("No valid username provided.");
      } else {
        String password = new BigInteger(130, new SecureRandom()).toString(32);
        if (mAccountService.setPassword(username, password)) {
          sendMail(username, MessageFormat.format(emails.getString("account.password.message"), password),
              emails.getString("account.password.subject"));
          result = ok(render("Password reset", "UserIndex/passwordReset.mustache"));
        } else {
          result = badRequest("Failed to reset password.");
        }
      }
    }

    return result;

  }

  public static Result newsletterSignup() {

    Map<String, Object> scope = new HashMap<>();
    scope.put("countries", Countries.list(OERWorldMap.mLocale));
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
      return internalServerError("Newsletter currently not available.");
    }

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

    return ok(username + " signed up for newsletter.");

  }

  private static void sendMail(String aEmailAddress, String aMessage, String aSubject) {
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
      mail.setSubject(aSubject);
      mail.addTo(aEmailAddress);
      mail.send();
      Logger.debug(mail.toString());
      Logger.info("Sent\n" + aMessage + "\nto " + aEmailAddress);
    } catch (EmailException e) {
      Logger.error(e.toString());
      Logger.debug("Failed to send\n" + aMessage + "\nto " + aEmailAddress);
    }
  }

  private static void createProfile(String aEmailAddress) throws IOException {

    // Check if person entry with corresponding email already exists
    List<Resource> users = mBaseRepository.getResources("about.email", aEmailAddress);
    for (Resource user : users) {
      if (user.getType().equals("Person")) {
        Logger.warn("Profile for ".concat(aEmailAddress).concat(" already exists."));
        mAccountService.setProfileId(aEmailAddress, user.getId());
        mAccountService.setPermissions(user.getId(), aEmailAddress);
        return;
      }
    }

    Resource person = new Resource("Person");
    person.put(JsonLdConstants.CONTEXT, "http://schema.org/");
    mBaseRepository.addResource(person, getMetadata());
    mAccountService.setProfileId(aEmailAddress, person.getId());
    mAccountService.setPermissions(person.getId(), aEmailAddress);

  }

}
