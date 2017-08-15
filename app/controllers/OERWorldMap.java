package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.maxmind.geoip2.DatabaseReader;
import helpers.JSONForm;
import helpers.JsonLdConstants;
import models.Resource;
import models.TripleCommit;
import org.apache.commons.lang3.StringUtils;
import play.Configuration;
import play.Environment;
import play.Logger;
import play.i18n.Lang;
import play.mvc.Controller;
import services.AccountService;
import services.QueryContext;
import services.repository.BaseRepository;
import services.repository.ElasticsearchRepository;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * @author fo
 */
//FIXME: re-enable Authorized.class, currently solved by getHttpBasicAuthUser()
// see https://github.com/playframework/playframework/issues/4706
//@With(Authorized.class)
public abstract class OERWorldMap extends Controller {

  Configuration mConf;
  Environment mEnv;
  static BaseRepository mBaseRepository;
  static AccountService mAccountService;
  static DatabaseReader mLocationLookup;
  static ObjectMapper mObjectMapper = new ObjectMapper();
  static JsonNode mSchemaNode;


  private static synchronized void createBaseRepository(Configuration aConf) {
    if (mBaseRepository == null) {
      try {
        mBaseRepository = new BaseRepository(aConf.underlying(), new ElasticsearchRepository(aConf.underlying()));
      } catch (final Exception ex) {
        throw new RuntimeException("Failed to create Respository", ex);
      }
    }
  }

  private static synchronized void createAccountService(Configuration aConf) {
    if (mAccountService == null) {
      mAccountService = new AccountService(
        new File(aConf.getString("user.token.dir")),
        new File(aConf.getString("ht.passwd")),
        new File(aConf.getString("ht.groups")),
        new File(aConf.getString("ht.profiles")),
        new File(aConf.getString("ht.permissions")));
      mAccountService.setApache2Ctl(aConf.getString("ht.apache2ctl.restart"));
    }
  }

  private static synchronized void createLocationLookup(Environment aEnv) {
    if (mLocationLookup == null) {
      try {
        mLocationLookup = new DatabaseReader.Builder(aEnv.resourceAsStream("GeoLite2-Country.mmdb")).build();
      } catch (final IOException ex) {
        throw new RuntimeException("Failed to create location lookup", ex);
      }
    }
  }

  private static synchronized void createSchemaNode(Configuration aConf) {
    try {
      mSchemaNode = new ObjectMapper().readTree(Paths.get(aConf.getString("json.schema")).toFile());
    } catch (IOException e) {
      Logger.error("Could not read schema", e);
    }
  }

  @Inject
  public OERWorldMap(Configuration aConf, Environment aEnv) {

    mConf = aConf;
    mEnv = aEnv;

    // Repository
    createBaseRepository(mConf);

    // Account service
    createAccountService(mConf);

    // Location lookup
    createLocationLookup(mEnv);

    // JSON schema
    createSchemaNode(mConf);

  }

  boolean getEmbed() {

    return ctx().request().queryString().containsKey("embed");

  }

  public Locale getLocale() {

    Locale locale = new Locale("en", "US");
    if (mConf.getBoolean("i18n.enabled")) {
      List<Lang> acceptLanguages = request().acceptLanguages();
      if (acceptLanguages.size() > 0) {
        locale = acceptLanguages.get(0).toLocale();
      }
    }

    return locale;

  }

  Resource getUser() {

    Resource user = null;
    Logger.trace("Username " + getHttpBasicAuthUser());
    String profileId = mAccountService.getProfileId(getHttpBasicAuthUser());
    if (!StringUtils.isEmpty(profileId)) {
      user = getRepository().getResource(profileId);
    }

    return user;

  }

  String getHttpBasicAuthUser() {

    String authHeader = ctx().request().getHeader(AUTHORIZATION);

    if (null == authHeader) {
      return null;
    }

    String auth = authHeader.substring(6);
    byte[] decoded = Base64.getDecoder().decode(auth);

    String[] credentials;
    try {
      credentials = new String(decoded, "UTF-8").split(":");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      return null;
    }

    if (credentials.length != 2) {
      return null;
    }

    return credentials[0];

  }

  QueryContext getQueryContext() {

    List<String> roles = new ArrayList<>();
    roles.add("guest");
    if (getUser() != null) {
      roles.add("authenticated");
    }

    return new QueryContext(roles);

  }

  ResourceBundle getEmails() {

    return ResourceBundle.getBundle("emails", getLocale());

  }

  String getLocation() {

    try {
      return mLocationLookup.country(InetAddress.getByName(request().remoteAddress())).getCountry().getIsoCode();
    } catch (Exception ex) {
      Logger.trace("Could not read host", ex);
      return "GB";
    }

  }

  ProcessingReport validate(Resource aResource) {
    ProcessingReport report = new ListProcessingReport();
    try {
      String type = aResource.getAsString(JsonLdConstants.TYPE);
      if (null == type) {
        report.error(new ProcessingMessage()
          .setMessage("No type found for ".concat(this.toString()).concat(", cannot validate")));
      } else if (null != mSchemaNode) {
        JsonSchema schema = JsonSchemaFactory.byDefault().getJsonSchema(mSchemaNode, "/definitions/".concat(type));
        report = schema.validate(aResource.toJson());
      } else {
        Logger.warn("No JSON schema present, validation disabled.");
      }
    } catch (ProcessingException e) {
      e.printStackTrace();
    }
    return report;
  }


  protected BaseRepository getRepository() {
    return mBaseRepository;
  }

  /**
   * Get resource from JSON body or form data
   * @return The JSON node
   */
  JsonNode getJsonFromRequest() {

    JsonNode jsonNode = ctx().request().body().asJson();
    if (jsonNode == null) {
      Map<String, String[]> formUrlEncoded = ctx().request().body().asFormUrlEncoded();
      if (formUrlEncoded != null) {
        jsonNode = JSONForm.parseFormData(formUrlEncoded, true);
      }
    }
    return jsonNode;

  }

  /**
   * Get metadata suitable for record provinence
   *
   * @return Map containing current getUser() in author and current time in date field.
   */
  protected Map<String, String> getMetadata() {

    Map<String, String> metadata = new HashMap<>();
    if (!StringUtils.isEmpty(getHttpBasicAuthUser())) {
      metadata.put(TripleCommit.Header.AUTHOR_HEADER, getHttpBasicAuthUser());
    } else {
      metadata.put(TripleCommit.Header.AUTHOR_HEADER, "System");
    }
    metadata.put(TripleCommit.Header.DATE_HEADER, ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    return metadata;

  }

}
