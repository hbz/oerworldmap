package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import helpers.JsonLdConstants;
import models.Resource;
import org.apache.commons.lang3.StringUtils;
import play.Configuration;
import play.Environment;
import play.Logger;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;

public class UserIndex extends OERWorldMap {

  @Inject
  public UserIndex(Configuration aConf, Environment aEnv) {
    super(aConf, aEnv);
  }

  public Result getProfile() {
    String profileId = (String) ctx().args.get("profile");
    if (StringUtils.isEmpty(profileId)) {
      return notFound();
    }
    Resource profile = mBaseRepository.getResource(profileId);
    if (profile == null) {
      Logger.warn("No profile for " + request().username());
      return notFound();
    }
    ObjectNode result = mObjectMapper.createObjectNode();
    result.put("username", request().username());
    String groups = (String) ctx().args.get("groups");
    if (StringUtils.isEmpty(groups)) {
      result.set("groups", mObjectMapper.createArrayNode());
    } else {
      result.set("groups", mObjectMapper.valueToTree(groups.split(",")));
    }
    result.put("id", profile.getId());
    result.set("name", profile.toJson().get("name"));
    return ok(result);
  }

  public Result createProfile() {
    String id = Arrays.stream(request().body().asFormUrlEncoded().get("id")).findFirst().orElse(null);
    String name = Arrays.stream(request().body().asFormUrlEncoded().get("name")).findFirst().orElse(null);
    String email = Arrays.stream(request().body().asFormUrlEncoded().get("email")).findFirst().orElse(null);
    String country = Arrays.stream(request().body().asFormUrlEncoded().get("country")).findFirst().orElse(null);
    String username = Arrays.stream(request().body().asFormUrlEncoded().get("username")).findFirst().orElse(null);
    ObjectNode profile = mObjectMapper.createObjectNode()
      .put(JsonLdConstants.CONTEXT, mConf.getString("jsonld.context"))
      .put("@type", "Person")
      .put("@id", id)
      .put("email", email);
    profile.set("name", mObjectMapper.createArrayNode()
      .add(mObjectMapper.createObjectNode()
        .put("@language", "en")
        .put("@value", name)
      )
    );
    profile.set("location", mObjectMapper.createObjectNode()
      .set("address", mObjectMapper.createObjectNode()
        .put("addressCountry", country)
      )
    );

    try {
      mBaseRepository.addResource(Resource.fromJson(profile), getMetadata());
      mAccountService.setPermissions(id, username);
      return created(profile);
    } catch (IOException e) {
      return internalServerError();
    }
  }

}
