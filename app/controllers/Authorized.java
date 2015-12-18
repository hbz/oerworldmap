package controllers;

import models.Resource;
import org.apache.commons.lang3.StringUtils;
import play.Logger;
import play.Play;
import play.Routes;
import play.libs.F;
import play.mvc.*;
import services.Account;
import services.QueryContext;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author fo
 */
public class Authorized extends Action.Simple {

  public static final String REALM = "Basic realm=\"OER World Map\"";
  public static final String AUTHORIZATION = "authorization";
  public static final String WWW_AUTHENTICATE = "WWW-Authenticate";

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
    String username = getHttpBasicAuthUser(ctx);

    Resource user;

    if (username != null) {
      ctx.request().setUsername(username);
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
      // Show prompt for users that are not logged in.
      if (StringUtils.isEmpty(user.getAsString("email"))) {
        ctx.response().setHeader(WWW_AUTHENTICATE, REALM);
        return F.Promise.pure(Results.unauthorized(OERWorldMap.render("Not authenticated", "Secured/token.mustache")));
      } else {
        return F.Promise.pure(Results.forbidden(OERWorldMap.render("Not authenticated", "Secured/token.mustache")));
      }

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

  private static String getHttpBasicAuthUser(Http.Context ctx) {

    String authHeader = ctx.request().getHeader(AUTHORIZATION);

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

    String username = credentials[0];
    String password = credentials[1];

    if (!Account.authenticate(username, password)) {
      return null;
    }

    return username;

  }

}
