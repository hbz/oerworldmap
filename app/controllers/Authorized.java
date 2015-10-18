package controllers;

import helpers.JsonLdConstants;
import models.Resource;
import org.elasticsearch.index.query.FilterBuilder;
import play.Logger;
import play.Play;
import play.Routes;
import play.libs.F;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import services.AggregationProvider;
import services.QueryContext;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author fo
 */
public class Authorized extends Action.Simple {

  private static Properties mPermissions;

  static {
    mPermissions = new Properties();
    try {
      mPermissions.load(Play.application().classloader().getResourceAsStream("permissions.properties"));
    } catch (IOException e) {
      Logger.error(e.toString());
    }
  }

  @Override
  public F.Promise<Result> call(Http.Context ctx) throws Throwable {

    String activity = ctx.args.get(Routes.ROUTE_CONTROLLER).toString()
        .concat(".").concat(ctx.args.get(Routes.ROUTE_ACTION_METHOD).toString());
    String username = Secured.getHttpBasicAuthUser(ctx);
    Resource user = null;

    if (username != null) {
      List<Resource> users = OERWorldMap.getRepository().getResources("about.email", username);
      if (users.size() == 1) {
        user = users.get(0);
        ctx.args.put("user", user);
      }
    }

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
    if (user != null && getUserActivities(user.getId(), parameters).contains(activity)) {
      queryContext = new QueryContext(user.getId(), getUserRoles(user.getId(), parameters));
      System.out.println("Authorized " + username + " for " + activity);
    } else {
      if (null == user) {
        queryContext = new QueryContext(null, getUserRoles(null, parameters));
      } else {
        queryContext = new QueryContext(user.getId(), getUserRoles(null, parameters));
      }
      System.out.println("Unuthorized " + username + " for " + activity);
    }
    ctx.args.put("queryContext", queryContext);

    return delegate.call(ctx);

  }

  public List<String> getUserActivities(String user, Map<String, String> parameters) {
    List<String> activities = new ArrayList<>();
    for (String role : getUserRoles(user, parameters)) {
      List<String> roleActivities = getRoleActivities(role);
      activities.addAll(roleActivities);
    }
    return activities;
  }

  public List<String> getUserRoles(String user, Map<String, String> parameters) {
    List<String> roles = new ArrayList<>();
    if (user == null) {
      roles.add("guest");
    } else {
      roles.add("authenticated");
      if (user.equals(parameters.get("id"))) {
        roles.add("owner");
      }
    }
    System.out.println(roles);
    return roles;
  }

  public List<String> getRoleActivities(String role) {
    List<String> activities = new ArrayList<>();
    for(Map.Entry<Object, Object> activity : mPermissions.entrySet()) {
      if (Arrays.asList(activity.getValue().toString().split(" +")).contains(role)) {
        activities.add(activity.getKey().toString());
      }
    }
    return activities;
  }

}
