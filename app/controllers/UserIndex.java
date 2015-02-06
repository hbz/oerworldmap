package controllers;

import java.io.*;
import java.nio.file.Paths;

import helpers.JsonLdConstants;
import io.michaelallen.mustache.MustacheFactory;
import io.michaelallen.mustache.api.Mustache;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;
import org.apache.commons.mail.EmailException;

import org.apache.commons.validator.routines.EmailValidator;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.client.Client;

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;

import org.elasticsearch.common.transport.InetSocketTransportAddress;

import play.Configuration;
import play.Play;
import play.data.DynamicForm;
import play.data.Form;

import play.data.validation.ValidationError;
import play.mvc.Controller;
import play.mvc.Result;

import models.Resource;
import play.twirl.api.Html;
import services.*;
import org.elasticsearch.node.Node;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.aggregations.AggregationBuilders;

import helpers.UniversalFunctions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

public class UserIndex extends Controller {
  
  private static Configuration mConf = Play.application().configuration();

  private static Settings clientSettings = ImmutableSettings.settingsBuilder()
      .put(new ElasticsearchConfig().getClientSettings()).build();
  private static Client mClient = new TransportClient(clientSettings)
      .addTransportAddress(new InetSocketTransportAddress(new ElasticsearchConfig().getServer(),
          9300));
  private static ElasticsearchClient mElasticsearchClient = new ElasticsearchClient(mClient);
  private static ElasticsearchRepository mUserRepository = new ElasticsearchRepository(
      mElasticsearchClient);
  
  private static FileResourceRepository mUnconfirmedUserRepository;
  static {
    try{
      mUnconfirmedUserRepository = new FileResourceRepository(Paths.get(mConf.getString("filerepo.dir")));
    }catch(final Exception ex){
      throw new RuntimeException("Failed to create FileResourceRespository", ex);
    }
  }
          

  public static Result get() throws IOException {
    Map data = new HashMap<>();
    data.put("countries", countryCodesDummyList());
    Mustache template = MustacheFactory.compile("UserIndex/index.mustache");
    Writer writer = new StringWriter();
    template.execute(writer, data);
    return ok(views.html.main.render("Registration", Html.apply(writer.toString())));
  }

  public static Result post() throws IOException {
    
    DynamicForm requestData = Form.form().bindFromRequest();
    
    if (requestData.hasErrors()) {
      
      Map data = new HashMap<>();
      data.put("countries", countryCodesDummyList());
      Mustache template = MustacheFactory.compile("UserIndex/index.mustache");
      Writer writer = new StringWriter();
      template.execute(writer, data);
      return badRequest(views.html.main.render("Registration", Html.apply(writer.toString())));
      
    } else {
      
      // Store user data
      Resource user = new Resource("Person");
      String email = requestData.get("email");
      String countryCode = requestData.get("address.addressCountry");
      
      List<ValidationError> validationErrors = checkEmailAddress(email);
      validationErrors.addAll(checkCountryCode(countryCode));
      
      if (!validationErrors.isEmpty()) {
        Map data = new HashMap<>();
        data.put("status", "warning");
        data.put("message", errorsToHtml(validationErrors));
        Mustache template = MustacheFactory.compile("feedback.mustache");
        Writer writer = new StringWriter();
        template.execute(writer, data);
        return badRequest(views.html.main.render("Registration", Html.apply(writer.toString())));
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
          Map data = new HashMap<>();
          data.put("link", routes.UserIndex.post().absoluteURL(request()) + user.get("@id"));
          Mustache template = MustacheFactory.compile("UserIndex/confirmation.mustache");
          Writer writer = new StringWriter();
          template.execute(writer, data);
          System.out.println(writer.toString());
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
        
        Map data = new HashMap<>();
        data.put("status", "success");
        data.put("message", "Thank you for your interest in the OER World Map. Your email address <em>"
                + user.get("email") + "</em> has been registered."
        );
        Mustache template = MustacheFactory.compile("feedback.mustache");
        Writer writer = new StringWriter();
        template.execute(writer, data);
        return ok(views.html.main.render("Registration", Html.apply(writer.toString())));
      }
    }
  }

  private static List<ValidationError> checkEmailAddress(String aEmail) {

    List<ValidationError> errors = new ArrayList<ValidationError>();

    if (!mUserRepository.getResourcesByContent("Person", "email", aEmail).isEmpty()) {
      errors.add(new ValidationError("email", "This e-mail is already registered."));
    } else if (!EmailValidator.getInstance().isValid(aEmail)) {
      errors.add(new ValidationError("email", "This is not a valid e-mail adress."));
    }
    return errors;
  }
  
  private static List<ValidationError> checkCountryCode(String aCountryCode) {

    List<ValidationError> errors = new ArrayList<ValidationError>();

    if (!countryCodesDummyList().contains(aCountryCode.toUpperCase())) {
      errors.add(new ValidationError("countryName", "This country is not valid."));
    }
    return errors;
  }

  private static List<String> countryCodesDummyList() {
    List<String> countryCodes = new ArrayList<String>();
    String country;
    try {
      BufferedReader in = new BufferedReader(new FileReader("conf/countryCodes.dummyList"));
      while ((country = in.readLine()) != null) {
        countryCodes.add(country);
      }
      in.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return countryCodes;
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
    Map data = new HashMap<>();
    Mustache template = MustacheFactory.compile("feedback.mustache");
    Writer writer = new StringWriter();
    
    try {
      user = mUnconfirmedUserRepository.deleteResource(id);
    } catch (IOException e) {
      e.printStackTrace();
      data.put("status", "warning");
      data.put("message", "Error confirming email address");
      template.execute(writer, data);
      return ok(views.html.main.render("Registration", Html.apply(writer.toString())));
    }

    mUserRepository.addResource(user);
    data.put("status", "success");
    data.put("message", "Thank you for your interest in the OER World Map. Your email address <em>"
            + user.get("email") + "</em> has been confirmed."
    );
    template.execute(writer, data);
    return ok(views.html.main.render("Registration", Html.apply(writer.toString())));
    
  }

}
