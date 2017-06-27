package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import helpers.JSONForm;
import helpers.JsonLdConstants;
import helpers.UniversalFunctions;
import models.GraphHistory;
import models.Resource;
import models.TripleCommit;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.commons.mail.SimpleEmail;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;
import play.Configuration;
import play.Environment;
import play.Logger;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class UserIndex extends OERWorldMap {

  private File mProfiles;
  private GraphHistory mConsents;

  @Inject
  public UserIndex(Configuration aConf, Environment aEnv) {
    super(aConf, aEnv);
    mProfiles = new File(mConf.getString("user.profile.dir"));
    mConsents = new GraphHistory(new File(mConf.getString("consents.history.dir")),
      new File(mConf.getString("consents.history.file")));
  }

  public Result signup() throws IOException {

    Map<String, Object> scope = new HashMap<>();
    scope.put("countries", UniversalFunctions.resourceBundleToMap(ResourceBundle
      .getBundle("iso3166-1-alpha-2", getLocale())));
    return ok(mObjectMapper.writeValueAsString(scope));

  }

  public Result register() throws IOException {

    Resource user = Resource.fromJson(JSONForm.parseFormData(ctx().request().body().asFormUrlEncoded()));
    user.put(JsonLdConstants.CONTEXT, mConf.getString("jsonld.context"));

    String username = user.getAsString("email");
    String password = user.getAsString("password");
    String confirm = user.getAsString("password-confirm");

    boolean privacyPolicyAccepted = user.getAsString("privacy-policy-accepted") != null;
    boolean termsOfServiceAccepted = user.getAsString("terms-of-service-accepted") != null;
    boolean emailToProfile = user.getAsString("add-email") != null;
    boolean registerNewsletter = user.getAsString("register-newsletter") != null;

    user.remove("password");
    user.remove("password-confirm");
    user.remove("privacy-policy-accepted");
    user.remove("terms-of-service-accepted");
    user.remove("add-email");
    user.remove("register-newsletter");

    ProcessingReport processingReport = validate(user);

    Result result;

    if (StringUtils.isEmpty(username)) {
      result = badRequest("No email address provided.");
    } else if (StringUtils.isEmpty(password)) {
      result = badRequest("No password provided.");
    } else if (!password.equals(confirm)) {
      result = badRequest("Passwords must match.");
    } else if (password.length() < 8) {
      result = badRequest("Password must be at least 8 characters long.");
    } else if (!privacyPolicyAccepted || !termsOfServiceAccepted) {
      result = badRequest("Please accept our privacy policy and the terms of service.");
    } else if (!processingReport.isSuccess()) {
      result = badRequest(processingReport.toString());
    } else {
      String token = mAccountService.addUser(username, password);
      if (token == null) {
        result = badRequest("Failed to add " . concat(username));
      } else {
        user.put("add-email", emailToProfile);
        try {
          logConsents(username, request().remoteAddress());
          saveProfile(token, user);
        } catch (IOException e) {
          Logger.error("Failed to create profile", e);
          return badRequest("An error occurred");
        }
        File termsOfService = new File("public/pdf/Terms_of_Service.pdf");
        String message;
        try {
          message = new String(getEmails().getString("account.verify.message")
            .getBytes("ISO-8859-1"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
          message = getEmails().getString("account.verify.message");
        }
        message = MessageFormat.format(message, mConf.getString("proxy.host")
          .concat(routes.UserIndex.verify(token).url()));
        sendMail(username, message, getEmails().getString("account.verify.subject"), new File[]{termsOfService});
        Map<String, Object> scope = new HashMap<>();
        scope.put("username", username);
        scope.put("newsletter", registerNewsletter);
        if (registerNewsletter && !registerNewsletter(username)) {
          Logger.error("Error registering newsletter for " + username);
        }
        result = ok(mObjectMapper.writeValueAsString(scope));
      }
    }

    return result;

  }

  public Result verify(String token) throws IOException {

    Result result;

    if (token == null) {
      result = badRequest("No token given.");
    } else {
      String username = mAccountService.verifyToken(token);
      if (username != null) {
        saveProfileToDb(token);
        Map<String, Object> scope = new HashMap<>();
        scope.put("username", username);
        String userId = mAccountService.getProfileId(username);
        scope.put("id", userId);
        String profileUrl = mConf.getString("proxy.host").concat(
          routes.ResourceIndex.read(userId, "HEAD", null).url());
        scope.put("url", profileUrl);
        result = ok(mObjectMapper.writeValueAsString(scope));
      } else {
        result = badRequest("Invalid token ".concat(token));
      }
    }

    return result;

  }

  public Result requestPassword() throws IOException {
    return ok(mObjectMapper.writeValueAsString(new HashMap<>()));
  }

  public Result sendPassword() throws IOException {

    Result result;

    Resource user = Resource.fromJson(JSONForm.parseFormData(ctx().request().body().asFormUrlEncoded()));

    String username;
    if (request().username() != null) {
      username = request().username();
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
        result = ok(mObjectMapper.writeValueAsString(new HashMap<>()));
      }
    } else {
      username = user.getAsString("email");
      if (StringUtils.isEmpty(username) || !mAccountService.userExists(username)) {
        result = badRequest("No valid username provided.");
      } else {
        String password = new BigInteger(130, new SecureRandom()).toString(32);
        if (mAccountService.setPassword(username, password)) {
          sendMail(username, MessageFormat.format(getEmails().getString("account.password.message"), password),
              getEmails().getString("account.password.subject"), null);
          result = ok(mObjectMapper.writeValueAsString(new HashMap<>()));
        } else {
          result = badRequest("Failed to reset password.");
        }
      }
    }

    return result;

  }

  public Result newsletterSignup() throws IOException {

    return ok(mObjectMapper.writeValueAsString(new HashMap<>()));

  }

  public Result newsletterRegister() {

    Resource user = Resource.fromJson(JSONForm.parseFormData(ctx().request().body().asFormUrlEncoded()));

    if (!validate(user).isSuccess()) {
      return badRequest("Please provide a valid email address and select a country.");
    }

    String email = user.getAsString("email");

    if (StringUtils.isEmpty(email)) {
      return badRequest("Not username given.");
    }

    return registerNewsletter(email)
      ? ok(email + " signed up for newsletter.")
      : internalServerError("Newsletter currently not available.");

  }

  public Result editGroups() throws IOException {

    Map<String, Map<String, Boolean>> groups = new HashMap<>();
    for (String group : mAccountService.getGroups()) {
      Map<String, Boolean> users = new HashMap<>();
      for (String user: mAccountService.getUsers()) {
        users.put(user, mAccountService.getUsers(group).contains(user));
      }
      groups.put(group, users);
    }

    Map<String, Object> scope = new HashMap<>();
    scope.put("groups", groups);
    return ok(mObjectMapper.writeValueAsString(scope));

  }

  public Result setGroups() throws IOException {

    Map<String, List<String>> groupUsers = new HashMap<>();

    if (ctx().request().body().asFormUrlEncoded() != null) {
      JsonNode jsonNode = JSONForm.parseFormData(ctx().request().body().asFormUrlEncoded());
      Iterator<String> groupNames = jsonNode.fieldNames();
      while (groupNames.hasNext()) {
        String group = groupNames.next();
        List<String> users = StreamSupport.stream(
          Spliterators.spliteratorUnknownSize(jsonNode.get(group).fieldNames(),
            Spliterator.ORDERED), false).collect(
          Collectors.toList());
        groupUsers.put(group, users);
      }
    }

    if (mAccountService.setGroups(groupUsers)) {
      return ok(mObjectMapper.writeValueAsString(new HashMap<>()));
    } else {
      return internalServerError("Failed to update groups");
    }

  }

  private void sendMail(String aEmailAddress, String aMessage, String aSubject, File[] attachments) {

    try {
      Email mail;
      if (attachments != null && attachments.length > 0) {
        mail = new MultiPartEmail();
        for (File attachment : attachments) {
          ((MultiPartEmail)mail).attach(attachment);
        }
      } else {
        mail = new SimpleEmail();
      }
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
      Logger.info("Sent\n" + aMessage + "\nto " + aEmailAddress);
    } catch (EmailException e) {
      Logger.error("Failed to send\n" + aMessage + "\nto " + aEmailAddress, e);
    }

  }

  private void saveProfile(String aToken, Resource aPerson) throws IOException {

    FileUtils.writeStringToFile(new File(mProfiles, aToken), aPerson.toString(), StandardCharsets.UTF_8);

  }

  private void saveProfileToDb(String aToken) throws IOException {

    File profile = new File(mProfiles, aToken);
    Resource person = Resource.fromJson(FileUtils.readFileToString(profile, StandardCharsets.UTF_8));
    if (person == null || !profile.delete()) {
      throw new IOException("Could not process profile for " + aToken);
    }
    String username = person.getAsString("email");
    boolean addEmailToProfile = (boolean) person.get("add-email");
    person.remove("add-email");
    if (!addEmailToProfile) {
      person.remove("email");
    }
    mBaseRepository.addResource(person, getMetadata());
    mAccountService.setPermissions(person.getId(), username);
    mAccountService.setProfileId(username, person.getId());

  }

  private boolean registerNewsletter(String aEmailAddress) {

    String mailmanHost = mConf.getString("mailman.host");
    String mailmanList = mConf.getString("mailman.list");
    if (mailmanHost.isEmpty() || mailmanList.isEmpty()) {
      Logger.warn("No mailman configured, user ".concat(aEmailAddress)
        .concat(" not signed up for newsletter"));
      return false;
    }

    HttpClient client = new DefaultHttpClient();
    HttpPost request = new HttpPost("https://" + mailmanHost + "/mailman/subscribe/" + mailmanList);
    try {
      List<NameValuePair> nameValuePairs = new ArrayList<>(1);
      nameValuePairs.add(new BasicNameValuePair("email", aEmailAddress));
      request.setEntity(new UrlEncodedFormEntity(nameValuePairs));
      HttpResponse response = client.execute(request);
      Integer responseCode = response.getStatusLine().getStatusCode();

      if (!responseCode.equals(200)) {
        throw new IOException(response.getStatusLine().toString());
      }

    } catch (IOException e) {
      Logger.error("Could not connect to mailman", e);
      return false;
    }

    return true;

  }

  private void logConsents(String aEmailAddress, String aIp) throws IOException {

    TripleCommit.Header header = new TripleCommit.Header(aIp, ZonedDateTime.now());
    TripleCommit.Diff diff = new TripleCommit.Diff();
    org.apache.jena.rdf.model.Resource subject = ResourceFactory.createResource("mailto:" + aEmailAddress);
    Property predicate = ResourceFactory.createProperty("info:accepted");
    diff.addStatement(ResourceFactory.createStatement(subject, predicate,
      ResourceFactory.createPlainLiteral("Terms Of Service")));
    diff.addStatement(ResourceFactory.createStatement(subject, predicate,
      ResourceFactory.createPlainLiteral("Privacy policy")));
    mConsents.add(new TripleCommit(header, diff));

  }

}
