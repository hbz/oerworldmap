package controllers;

import java.io.IOException;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;
import org.apache.commons.mail.EmailException;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.client.Client;

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;

import org.elasticsearch.common.transport.InetSocketTransportAddress;

import play.Configuration;
import play.Play;
import play.data.DynamicForm;
import play.data.Form;

import play.mvc.Controller;
import play.mvc.Result;

import models.Resource;
import services.ElasticsearchClient;
import services.ElasticsearchConfig;
import services.ElasticsearchRepository;

public class UserIndex extends Controller {

  private static Settings clientSettings = ImmutableSettings.settingsBuilder()
      .put(new ElasticsearchConfig().getClientSettings()).build();
  private static Client mClient = new TransportClient(clientSettings)
      .addTransportAddress(new InetSocketTransportAddress(new ElasticsearchConfig().getServer(),
          9300));
  private static ElasticsearchClient mElasticsearchClient = new ElasticsearchClient(mClient);
  private static ElasticsearchRepository resourceRepository = new ElasticsearchRepository(
      mElasticsearchClient);

  public static Result get() throws IOException {
    return ok(views.html.UserIndex.index.render());
  }

  public static Result post() throws IOException {
    
    DynamicForm requestData = Form.form().bindFromRequest();
    
    if (requestData.hasErrors()) {
      
      return badRequest(views.html.UserIndex.index.render());
      
    } else {
      
      // Store user data
      Resource user = new Resource("Person");
      user.put("email", requestData.get("email"));
      String countryCode = requestData.get("address.addressCountry");
      if (!"".equals(countryCode)) {
        Resource address = new Resource("PostalAddress");
        address.put("countryName", requestData.get("address.addressCountry"));
        user.put("address", address);
      }
      resourceRepository.addResource(user);

      // Send confirmation mail
      Email confirmationMail = new SimpleEmail();
      Configuration conf = Play.application().configuration();
      try {
        confirmationMail.setHostName(conf.getString("smtp.host"));
        confirmationMail.setSmtpPort(conf.getInt("smtp.port"));
        confirmationMail.setAuthenticator(new DefaultAuthenticator(conf.getString("smtp.user"), conf.getString("smtp.password")));
        confirmationMail.setSSLOnConnect(true);
        confirmationMail.setFrom("oerworldmap@gmail.com");
        confirmationMail.setSubject("Please confirm");
        confirmationMail.setMsg("Please confirm...");
        confirmationMail.addTo((String)user.get("email"));
        confirmationMail.send();
      } catch (EmailException e) {
        e.printStackTrace();
      }
      
      return ok(views.html.UserIndex.registered.render((String) user.get("email")));
      
    }
  }

}
