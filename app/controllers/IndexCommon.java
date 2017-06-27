package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import models.Resource;
import play.Configuration;
import play.Environment;
import play.mvc.Result;
import services.repository.BaseRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author pvb
 */

public abstract class IndexCommon extends OERWorldMap{

  public IndexCommon(Configuration aConf, Environment aEnv) {
    super(aConf, aEnv);
  }


  public Result addResource() throws IOException {
    JsonNode jsonNode = getJsonFromRequest();
    if (jsonNode == null || (!jsonNode.isArray() && !jsonNode.isObject())) {
      return badRequest("Bad or empty JSON");
    } else if (jsonNode.isArray()) {
      return upsertResources();
    } else {
      return upsertResource(false);
    }
  }


  abstract Result upsertResource(boolean aBoolean) throws IOException;


  abstract Result upsertResources() throws IOException;


  protected Result importResources(BaseRepository aBaseRepository) throws IOException {
    JsonNode json = ctx().request().body().asJson();
    List<Resource> resources = new ArrayList<>();
    if (json.isArray()) {
      for (JsonNode node : json) {
        resources.add(Resource.fromJson(node));
      }
    } else if (json.isObject()) {
      resources.add(Resource.fromJson(json));
    } else {
      return badRequest();
    }
    aBaseRepository.importResources(resources, getMetadata());
    return ok(Integer.toString(resources.size()).concat(" resources imported."));
  }


  protected Result updateResource(String aId, BaseRepository aBaseRepository) throws IOException {
    // If updating a resource, check if it actually exists
    Resource originalResource = aBaseRepository.getResource(aId);
    if (originalResource == null) {
      return notFound("Not found: ".concat(aId));
    }
    return upsertResource(true);
  }


  public Result label(String aId) {
    return ok(mBaseRepository.label(aId));
  }
}
