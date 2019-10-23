package services;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.UserRepresentation;
import play.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fo
 */
public class KeycloakAccountService implements AccountService {

  private static final String mLimitWriteDirective =
    "<Location /resource/%s>\n" +
      "  <LimitExcept GET>\n" +
      "    Require claim groups:admin\n" +
      "    Require user %s\n" +
      "  </LimitExcept>\n" +
      "</Location>";

  private String mApache2ctl = "sudo apache2ctl graceful";

  private final File mPermissionsDir;
  private RealmResource mRealm;

  public KeycloakAccountService(String aServerUrl, String aRealm, String aUsername, String aPassword, String aClientId,
    File aPermissionsDir) {
    mPermissionsDir = aPermissionsDir;
    mRealm = Keycloak.getInstance(aServerUrl, "master", aUsername, aPassword, aClientId).realm(aRealm);
  }

  public void setPermissions(String aId, String aUser) {
    String entry = String.format(mLimitWriteDirective, aId, aUser);
    String fileName = aId.substring(aId.lastIndexOf(":") + 1).trim();
    try {
      FileUtils
        .writeStringToFile(new File(mPermissionsDir, fileName), entry, StandardCharsets.UTF_8);
    } catch (IOException e) {
      Logger.error("Could not create permission file", e);
    }
    refresh();
  }

  public boolean removePermissions(String aId) {
    String fileName = aId.substring(aId.lastIndexOf(":") + 1).trim();
    boolean status = FileUtils.deleteQuietly(new File(mPermissionsDir, fileName));
    refresh();
    return status;
  }

  public void setApache2Ctl(String aApache2ctl) {
    mApache2ctl = aApache2ctl;
  }

  private void refresh() {
    try {
      Process apache2ctl = Runtime.getRuntime().exec(mApache2ctl);
      BufferedReader stdInput = new BufferedReader(
        new InputStreamReader(apache2ctl.getInputStream()));
      BufferedReader stdError = new BufferedReader(
        new InputStreamReader(apache2ctl.getErrorStream()));
      Logger.debug(IOUtils.toString(stdInput));
      Logger.debug(IOUtils.toString(stdError));
    } catch (IOException e) {
      Logger.error("Could not restart Apache", e);
    }
  }

  public boolean deleteUser(String username) {
    UserRepresentation user = getUser(username);
    if (user != null) {
      return mRealm.users().delete(user.getId()).getStatus() == 200;
    }
    return false;
  }

  public String getProfileId(String username) {
    UserRepresentation user = getUser(username);
    if (user != null && user.getAttributes() != null && user.getAttributes().containsKey("profile_id")) {
      return user.getAttributes().get("profile_id").get(0);
    }
    return null;
  }

  public void setProfileId(String username, String profileId) {
    UserRepresentation user = getUser(username);
    if (user != null) {
      if (user.getAttributes() != null) {
        user.getAttributes().put("profile_id", Collections.singletonList(profileId));
      } else {
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("profile_id", Collections.singletonList(profileId));
        user.setAttributes(attributes);
      }
      mRealm.users().get(user.getId()).update(user);
    }
  }

  public String getUsername(String profileId) {
    UserRepresentation user = mRealm.users().list().stream()
      .filter(u -> u.getAttributes() != null && u.getAttributes().containsKey("profile_id") && u.getAttributes().get("profile_id").contains(profileId))
      .findFirst().orElse(null);
    return user != null ? user.getUsername() : null;
  }

  private UserRepresentation getUser(String username) {
    return mRealm.users().search(username).stream()
      .filter(u -> u.getUsername().equals(username)).findFirst().orElse(null);
  }

}
