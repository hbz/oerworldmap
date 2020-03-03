package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import helpers.JsonSchemaValidator;
import models.Resource;
import models.TripleCommit;
import org.apache.commons.lang3.StringUtils;
import play.Configuration;
import play.Environment;
import play.Logger;
import play.mvc.Controller;
import play.mvc.With;
import services.AccountService;
import services.KeycloakAccountService;
import services.MemoryAccountService;
import services.QueryContext;
import services.repository.BaseRepository;
import services.repository.ElasticsearchRepository;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fo
 */
@With(Authorized.class)
public abstract class OERWorldMap extends Controller {

  Configuration mConf;
  Environment mEnv;
  static BaseRepository mBaseRepository;
  static AccountService mAccountService;
  static final ObjectMapper mObjectMapper = new ObjectMapper();
  private static JsonSchemaValidator mSchemaValidator;

  private static synchronized void createBaseRepository(Configuration aConf) {
    if (mBaseRepository == null) {
      try {
        mBaseRepository = new BaseRepository(aConf.underlying(),
          new ElasticsearchRepository(aConf.underlying()), mAccountService);
      } catch (final Exception ex) {
        throw new RuntimeException("Failed to create Repository", ex);
      }
    }
  }

  private static synchronized void createAccountService(Configuration aConf) {
    Configuration keycloakConfig = aConf.getConfig("keycloak");
    if (mAccountService == null) {
      if (keycloakConfig != null) {
        mAccountService = new KeycloakAccountService(
          keycloakConfig.getString("serverUrl"),
          keycloakConfig.getString("realm"),
          keycloakConfig.getString("username"),
          keycloakConfig.getString("password"),
          keycloakConfig.getString("client"),
          new File(aConf.getString("ht.permissions")));
        mAccountService.setApache2Ctl(aConf.getString("ht.apache2ctl.restart"));
      } else {
        mAccountService = new MemoryAccountService();
      }
    }
  }

  private static synchronized void createSchemaValidator(Configuration aConf) {
    try {
      mSchemaValidator = new JsonSchemaValidator(
        Paths.get(aConf.getString("json.schema")).toFile());
    } catch (IOException e) {
      Logger.error("Could not read schema", e);
    }
  }

  @Inject
  public OERWorldMap(Configuration aConf, Environment aEnv) {
    mConf = aConf;
    mEnv = aEnv;
    // Account service
    createAccountService(mConf);
    // Repository
    createBaseRepository(mConf);
    // JSON schema validator
    createSchemaValidator(mConf);
  }

  Resource getUser() {
    Resource user = null;
    Logger.trace("Username " + request().username());
    String profileId = mAccountService.getProfileId(request().username());
    if (!StringUtils.isEmpty(profileId)) {
      user = getRepository().getResource(profileId);
    }
    return user;
  }

  QueryContext getQueryContext() {
    List<String> roles = new ArrayList<>();
    roles.add("guest");
    if (getUser() != null) {
      roles.add("authenticated");
    }
    return new QueryContext(roles);
  }

  ProcessingReport validate(Resource aResource) {
    return mSchemaValidator.validate(aResource);
  }

  protected BaseRepository getRepository() {
    return mBaseRepository;
  }

  /**
   * Get resource from JSON body or form data
   *
   * @return The JSON node
   */
  JsonNode getJsonFromRequest() {
    return ctx().request().body().asJson();
  }

  /**
   * Get metadata suitable for record provinence
   *
   * @return Map containing current getUser() in author and current time in date field.
   */
  protected Map<String, String> getMetadata() {
    Map<String, String> metadata = new HashMap<>();
    if (!StringUtils.isEmpty(request().username())) {
      metadata.put(TripleCommit.Header.AUTHOR_HEADER, request().username());
    } else {
      metadata.put(TripleCommit.Header.AUTHOR_HEADER, "System");
    }
    metadata.put(TripleCommit.Header.DATE_HEADER,
      ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    return metadata;
  }

  public BaseRepository getBaseRepository() {
    return mBaseRepository;
  }
}
