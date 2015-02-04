package controllers;

import java.io.IOException;
import java.nio.file.Paths;

import helpers.JsonLdConstants;
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
import services.*;
import org.elasticsearch.node.Node;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.aggregations.AggregationBuilders;

import helpers.UniversalFunctions;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    return ok(views.html.UserIndex.index.render(countryCodesDummyList()));
  }

  public static Result post() throws IOException {
    
    DynamicForm requestData = Form.form().bindFromRequest();
    
    if (requestData.hasErrors()) {
      
      return badRequest(views.html.UserIndex.index.render(countryCodesDummyList()));
      
    } else {
      
      // Store user data
      Resource user = new Resource("Person");
      String email = requestData.get("email");
      String countryCode = requestData.get("address.addressCountry");
      
      List<ValidationError> validationErrors = checkEmailAddress(email);
      validationErrors.addAll(checkCountryCode(countryCode));
      
      if (!validationErrors.isEmpty()) {
        return badRequest("Data entry error: ".concat(UniversalFunctions
            .collectionToString(validationErrors)));
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
          confirmationMail.setHostName(mConf.getString("mail.smtp.host"));
          confirmationMail.setSmtpPort(mConf.getInt("mail.smtp.port"));
          confirmationMail.setAuthenticator(
                  new DefaultAuthenticator(mConf.getString("mail.smtp.user"), mConf.getString("mail.smtp.password"))
          );
          confirmationMail.setSSLOnConnect(true);
          confirmationMail.setFrom("oerworldmap@gmail.com");
          confirmationMail.setSubject("Please confirm");
          confirmationMail.setMsg(
                  views.txt.UserIndex.confirmation.render((String) user.get(JsonLdConstants.ID)).body()
          );
          confirmationMail.addTo((String)user.get("email"));
          confirmationMail.send();
        } catch (EmailException e) {
          e.printStackTrace();
        }

        return ok(views.html.UserIndex.registered.render((String) user.get("email")));
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
  
  public static Result confirm(String id) throws IOException {
    
    Resource user;
    
    try {
      user = mUnconfirmedUserRepository.deleteResource(id);
    } catch (IOException e) {
      e.printStackTrace();
      return ok("An error occurred for " + id);
    }

    mUserRepository.addResource(user);
    return ok("User confirmed: " + id);
    
  }

}
