package controllers;

import models.Resource;
import play.Logger;
import play.Play;
import play.Routes;
import play.libs.F;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import services.QueryContext;

import java.io.IOException;
import java.util.*;

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
    String user = Secured.getHttpBasicAuthUser(ctx);

    if (user != null) {
      List<Resource> users = OERWorldMap.getRepository().getResources("email", user);
      if (users.size() == 1) {
        ctx.args.put("user", users.get(0));
      }
    }

    if (getUserActivities(user).contains(activity)) {
      System.out.println("Authorized " + user);
    } else {
      System.out.println("Unuthorized");
    }

    ctx.args.put("queryContext", new QueryContext(ctx));

    return delegate.call(ctx);

  }

  public List<String> getUserActivities(String user) {
    List<String> activities = new ArrayList<>();
    for (String role : getUserRoles(user)) {
      List<String> roleActivities = getRoleActivities(role);
      activities.addAll(roleActivities);
    }
    return activities;
  }

  public List<String> getUserRoles(String user) {
    List<String> roles = new ArrayList<>();
    //roles.add("owner");
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
