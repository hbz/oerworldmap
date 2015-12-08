package controllers;

import models.Resource;
import org.apache.commons.lang3.StringUtils;
import play.Logger;
import play.Play;
import play.Routes;
import play.libs.F;
import play.mvc.*;
import services.QueryContext;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author fo
 */
public class Authorized extends Action.Simple {

  private static Map<String, List<String>> mPermissions;

  private static Map<String, List<String>> mRoles;

  static {
    mPermissions = new HashMap<>();
    try {
      Properties permissions = new Properties();
      permissions.load(Play.application().classloader().getResourceAsStream("permissions.properties"));
      for(Map.Entry<Object, Object> permission : permissions.entrySet()) {
        mPermissions.put(permission.getKey().toString(), new ArrayList<>(Arrays.asList(permission.getValue().toString()
          .split(" +"))));
      }
    } catch (IOException e) {
      Logger.error(e.toString());
    }
  }

  static {
    mRoles = new HashMap<>();
    try {
      Properties roles = new Properties();
      roles.load(Play.application().classloader().getResourceAsStream("roles.properties"));
      for(Map.Entry<Object, Object> role : roles.entrySet()) {
        mRoles.put(role.getKey().toString(), new ArrayList<>(Arrays.asList(role.getValue().toString().split(" +"))));
      }
    } catch (IOException e) {
      Logger.error(e.toString());
    }
  }

  @Override
  public F.Promise<Result> call(Http.Context ctx) throws Throwable {

    String activity = ctx.args.get(Routes.ROUTE_CONTROLLER).toString()
        .concat(".").concat(ctx.args.get(Routes.ROUTE_ACTION_METHOD).toString());
    String username = Secured.getHttpBasicAuthUser(ctx);

    Resource user;

    if (username != null) {
      List<Resource> users = OERWorldMap.getRepository().getResources("about.email", username);
      if (users.size() == 1) {
        user = users.get(0);
      } else {
        user = new Resource("Person", username);
        user.put("email", username);
      }
    } else {
      user = new Resource("Person");
    }

    ctx.args.put("user", user);

    if (mPermissions.get(activity) != null) {
      mPermissions.get(activity).add("admin");
    } else {
      List<String> permissions = new ArrayList<>();
      permissions.add("guest");
      permissions.add("admin");
      mPermissions.put(activity, permissions);
    }

    // Extract parameters via route pattern
    Pattern routePattern = Pattern.compile("\\$([^<]+)<([^>]+)>");
    Matcher routePatternMatcher = routePattern.matcher(ctx.args.get(Routes.ROUTE_PATTERN).toString());
    List<String> parameterNames = new ArrayList<>();
    while (routePatternMatcher.find()) {
      parameterNames.add(routePatternMatcher.group(1));
    }

    Map<String, String> parameters = new HashMap<>();
    if (!parameterNames.isEmpty()) {
      String regex = routePatternMatcher.replaceAll("($2)");
      Pattern path = Pattern.compile(regex);
      Matcher parts = path.matcher(ctx.request().path());
      int i = 0;
      while (parts.find()) {
        parameters.put(parameterNames.get(i), parts.group(1));
      }
    }

    QueryContext queryContext;
    if (getUserActivities(user, parameters).contains(activity)) {
      queryContext = new QueryContext(user.getId(), getUserRoles(user, parameters));
      Logger.info("Authorized " + user.getId() + " for " + activity + " with " + parameters);
    } else {
      Logger.warn("Unuthorized " + user.getId() + " for " + activity + " with " + parameters);
      ctx.response().setHeader(Secured.WWW_AUTHENTICATE, Secured.REALM);
      return F.Promise.pure(Results.unauthorized(OERWorldMap.render("Not authenticated", "Secured/token.mustache")));
    }
    ctx.args.put("queryContext", queryContext);

    return delegate.call(ctx);

  }

  public List<String> getUserActivities(Resource user, Map<String, String> parameters) {
    List<String> activities = new ArrayList<>();
    for (String role : getUserRoles(user, parameters)) {
      List<String> roleActivities = getRoleActivities(role);
      activities.addAll(roleActivities);
    }
    return activities;
  }

  public List<String> getUserRoles(Resource user, Map<String, String> parameters) {

    List<String> roles = new ArrayList<>();

    for(Map.Entry<String, List<String>> role : mRoles.entrySet()) {
      if (role.getValue().contains(user.getId())) {
        roles.add(role.getKey());
      }
    }

    roles.add("guest");

    if (user != null) {
      if (!StringUtils.isEmpty(user.getAsString("email"))) {
        roles.add("authenticated");
      }
      if (user.getId().equals(parameters.get("id"))) {
        roles.add("owner");
      }
    }

    return roles;

  }

  public List<String> getRoleActivities(String role) {
    List<String> activities = new ArrayList<>();
    for(Map.Entry<String, List<String>> activity : mPermissions.entrySet()) {
      if (activity.getValue().contains(role)) {
        activities.add(activity.getKey());
      }
    }
    return activities;
  }

}
