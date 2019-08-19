package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import helpers.JsonLdConstants;
import models.Record;
import models.Resource;
import org.apache.commons.lang3.StringUtils;
import play.Configuration;
import play.Environment;
import play.Logger;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;

public class UserIndex extends OERWorldMap {

  private static final String OIDC_CLAIM_PROFILE_ID = "oidc_claim_profile_id";
  private static final String OIDC_CLAIM_GROUPS = "oidc_claim_groups";
  private static final String OIDC_CLAIM_NAME = "oidc_claim_name";
  private static final String OIDC_CLAIM_SUB = "oidc_claim_sub";

  @Inject
  public UserIndex(Configuration aConf, Environment aEnv) {
    super(aConf, aEnv);
  }

  public Result getProfile() {
    String profileId = request().getHeader(OIDC_CLAIM_PROFILE_ID);
    Resource profile;
    boolean persistent = false;
    if (StringUtils.isEmpty(profileId)) {
      profile = newProfile();
    } else {
      profile = mBaseRepository.getResource(profileId);
      if (profile == null) {
        Logger.warn("No profile for " + profileId);
        profile = newProfile();
      } else {
        persistent = true;
      }
    }
    return ok(wrapProfile(profile, persistent));
  }

  public Result createProfile() throws IOException {
    Resource profile = Resource.fromJson(getJsonFromRequest());
    String username = request().username();
    boolean isNew = !mBaseRepository.hasResource(profile.getId());
    mBaseRepository.addResource(profile, getMetadata());
    if (isNew) {
      mAccountService.setProfileId(username, profile.getId());
      mAccountService.setPermissions(profile.getId(), username);
    }
    return isNew ? created(wrapProfile(profile, true)) : ok(wrapProfile(profile, true));
  }

  private ObjectNode wrapProfile(Resource profile, boolean persistent) {
    ObjectNode result = mObjectMapper.createObjectNode();
    result.put("persistent", persistent);
    result.put("username", request().username());
    String groups = request().getHeader(OIDC_CLAIM_GROUPS);
    if (StringUtils.isEmpty(groups)) {
      result.set("groups", mObjectMapper.createArrayNode());
    } else {
      result.set("groups", mObjectMapper.valueToTree(groups.split(",")));
    }
    result.set("profile", new Record(profile).toJson());
    result.put("id", profile.getId());
    return result;
  }

  private Resource newProfile() {
    ObjectNode profileNode = mObjectMapper.createObjectNode()
      .put(JsonLdConstants.CONTEXT, mConf.getString("jsonld.context"))
      .put("@type", "Person")
      .put("@id", "urn:uuid:".concat(request().getHeader(OIDC_CLAIM_SUB)))
      .put("email", request().username());
    profileNode.set("name", mObjectMapper.createObjectNode()
      .put("en", request().getHeader(OIDC_CLAIM_NAME))
    );
    return(Resource.fromJson(profileNode));
  }

}
