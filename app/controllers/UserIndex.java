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
import java.util.ArrayList;
import java.util.List;

import models.Resource;

import org.apache.commons.codec.digest.DigestUtils;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
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
      ensureEmailUnique(user, report);
    }
    if (!report.isSuccess()) {
      mResponseData.put("countries", Countries.list(currentLocale));
      mResponseData.put("errors", JSONForm.generateErrorReport(report));
      mResponseData.put("person", user);
      return badRequest(render("Registration", "UserIndex/index.mustache"));
    }

    newsletterSignup(user);
    user.put("email", getEncryptedEmailAddress(user));
    mUnconfirmedUserRepository.addResource(user);
    mResourceRepository.addResource(user);

    mResponseData.put("status", "success");
    mResponseData.put("message", i18n.get("user_registration_feedback"));
    return ok(render("Registration", "feedback.mustache"));

  }

  private static void ensureEmailUnique(Resource user, ProcessingReport aReport) {
    String aEmail = getEncryptedEmailAddress(user);
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

  private static String getEncryptedEmailAddress(Resource user) {
    return DigestUtils.sha256Hex(user.get("email").toString());
  }

  private static void newsletterSignup(Resource user) {
    HttpClient client = new DefaultHttpClient();
    HttpPost request = new HttpPost("https://" + mConf.getString("mailman.host") + "/mailman/subscribe/"
        + mConf.getString("mailman.list"));
    try {
      List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
      nameValuePairs.add(new BasicNameValuePair("email", user.get("email").toString()));
      request.setEntity(new UrlEncodedFormEntity(nameValuePairs));

      HttpResponse response = client.execute(request);
      System.out.println(response.getStatusLine().getStatusCode());
      BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
      String line = "";
      while ((line = rd.readLine()) != null) {
        System.out.println(line);
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
