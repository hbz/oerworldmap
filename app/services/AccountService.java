package services;

import helpers.MD5Crypt;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import play.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * @author fo
 */
public class AccountService {

  private static final String mLimitWriteDirective =
    "<Location /resource/%s>\n" +
      "  <LimitExcept GET>\n" +
      "    Require group admin\n" +
      "    Require user %s\n" +
      "  </LimitExcept>\n" +
      "</Location>";

  private String mApache2ctl = "sudo apache2ctl graceful";

  private final File mTokenDir;
  private final File mUserFile;
  private final File mGroupFile;
  private final File mProfileFile;
  private final File mPermissionsDir;

  public AccountService(File aTokenDir, File aUserFile, File aGroupFile, File aProfileFile,
    File aPermissionsDir) {

    mTokenDir = aTokenDir;
    mUserFile = aUserFile;
    mGroupFile = aGroupFile;
    mProfileFile = aProfileFile;
    mPermissionsDir = aPermissionsDir;
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

  public void refresh() {

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

  public String addUser(String username, String password) {

    if (!userExists(username) && !pendingVerification(username)) {
      try {
        String token = getEncryptedUsername(username);
        File tokenFile = new File(mTokenDir, token);
        FileUtils
          .writeStringToFile(tokenFile, buildEntry(username, password), StandardCharsets.UTF_8);
        return token;
      } catch (IOException e) {
        Logger.error("Could not write token file", e);
      }
    }

    return null;
  }

  public boolean deleteUser(String username) {

    if (!userExists(username)) {
      return false;
    }

    try {
      List<String> userDb = Files.readAllLines(mUserFile.toPath());
      for (final ListIterator<String> i = userDb.listIterator(); i.hasNext(); ) {
        if (i.next().startsWith(username)) {
          i.remove();
          break;
        }
      }
      FileUtils.writeLines(mUserFile, userDb);
      return true;
    } catch (IOException e) {
      Logger.error("Could not write user file", e);
      return false;
    }
  }

  public String verifyToken(String token) {

    File tokenFile = new File(mTokenDir, token);

    if (tokenFile.exists()) {
      try {
        String entry = FileUtils.readFileToString(tokenFile, StandardCharsets.UTF_8);
        new BufferedWriter(new FileWriter(mUserFile, true)).append(entry.concat("\n")).close();
        FileUtils.forceDelete(tokenFile);
        return entry.split(":")[0];
      } catch (IOException e) {
        Logger.error("Could not process token file", e);
      }
    }

    return null;
  }

  public boolean userExists(String username) {

    try {
      return FileUtils.readFileToString(mUserFile, StandardCharsets.UTF_8).contains(username);
    } catch (IOException e) {
      Logger.error("Could not read user file", e);
    }

    return false;
  }

  public boolean pendingVerification(String username) {
    return new File(mTokenDir, getEncryptedUsername(username)).exists();
  }

  public boolean updatePassword(String username, String current, String updated) {

    return validatePassword(username, current) && setPassword(username, updated);
  }

  public boolean setPassword(String username, String password) {

    if (!userExists(username)) {
      return false;
    }

    try {
      List<String> userDb = Files.readAllLines(mUserFile.toPath());
      for (final ListIterator<String> i = userDb.listIterator(); i.hasNext(); ) {
        if (i.next().startsWith(username)) {
          i.set(buildEntry(username, password));
          break;
        }
      }
      FileUtils.writeLines(mUserFile, userDb);
      return true;
    } catch (IOException e) {
      Logger.error("Could not read user file", e);
      return false;
    }
  }

  public boolean validatePassword(String username, String password) {

    if (!userExists(username)) {
      return false;
    }

    String entry = getEntry(username);
    return entry != null && MD5Crypt.verifyPassword(password, entry.split(":")[1]);
  }

  // FIXME: unit tests
  public List<String> getGroups(String username) {

    List<String> groups = new ArrayList<>();

    try {
      List<String> lines = Files.readAllLines(mGroupFile.toPath());
      for (String line : lines) {
        String[] entry = line.split(":");
        String group = entry[0].trim();
        List<String> users =
          entry.length > 1 ? Arrays.asList(entry[1].split(" +")) : new ArrayList<>();
        if (users.contains(username)) {
          groups.add(group);
        }
      }
    } catch (IOException e) {
      Logger.error("Could not read group file", e);
    }

    return groups;
  }

  public List<String> getGroups() {

    List<String> groups = new ArrayList<>();

    try {
      List<String> lines = Files.readAllLines(mGroupFile.toPath());
      for (String line : lines) {
        String[] entry = line.split(":");
        String group = entry[0].trim();
        groups.add(group);
      }
    } catch (IOException e) {
      Logger.error("Could not read group file", e);
    }

    return groups;
  }

  /**
   * Update all groups at once.
   *
   * @param groups The map of group name with all users of that group
   * @return True on success
   */
  public boolean setGroups(Map<String, List<String>> groups) {

    List<String> lines = new ArrayList<>();

    for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
      lines.add(entry.getKey().concat(": ").concat(String.join(" ", entry.getValue())));
    }

    try {
      FileUtils.writeLines(mGroupFile, lines);
      return true;
    } catch (IOException e) {
      Logger.error("Could not write group file", e);
      return false;
    }
  }

  public List<String> getUsers() {

    List<String> users = new ArrayList<>();

    try {
      List<String> lines = Files.readAllLines(mUserFile.toPath());
      for (String line : lines) {
        String[] entry = line.split(":");
        String user = entry[0].trim();
        users.add(user);
      }
    } catch (IOException e) {
      Logger.error("Could not read user file", e);
    }

    return users;
  }

  public List<String> getUsers(String aGroup) {

    try {
      List<String> lines = Files.readAllLines(mGroupFile.toPath());
      for (String line : lines) {
        String[] entry = line.split(":");
        String group = entry[0].trim();
        if (group.equals(aGroup)) {
          return Arrays.asList(entry[1].split(" +"));
        }
      }
    } catch (IOException e) {
      Logger.error("Could not read user file", e);
    }

    return new ArrayList<>();
  }


  public boolean setProfileId(String username, String profileId) {

    if (!userExists(username) && !StringUtils.isEmpty(profileId)) {
      return false;
    }

    try {
      List<String> profileDb = Files.readAllLines(mProfileFile.toPath());
      for (final ListIterator<String> i = profileDb.listIterator(); i.hasNext(); ) {
        if (i.next().startsWith(username)) {
          if (StringUtils.isEmpty(profileId)) {
            i.remove();
          } else {
            i.set(username.concat(" ").concat(profileId));
          }
          FileUtils.writeLines(mProfileFile, profileDb);
          return true;
        }
      }
      if (!StringUtils.isEmpty(profileId)) {
        FileUtils.writeLines(mProfileFile,
          Collections.singletonList(username.concat(" ").concat(profileId)), true);
      }
      return true;
    } catch (IOException e) {
      Logger.error("Could not write profile file", e);
      return false;
    }
  }

  public String getProfileId(String username) {

    try {
      List<String> lines = Files.readAllLines(mProfileFile.toPath());
      for (String line : lines) {
        String[] entry = line.split(" ");
        String name = entry[0].trim();
        if (name.equals(username)) {
          return entry[1].trim();
        }
      }
    } catch (IOException e) {
      Logger.error("Could not read profile file", e);
    }

    return null;
  }

  public String getUsername(String profileId) {

    try {
      List<String> lines = Files.readAllLines(mProfileFile.toPath());
      for (String line : lines) {
        String[] entry = line.split(" ");
        String id = entry[1].trim();
        if (id.equals(profileId)) {
          return entry[0].trim();
        }
      }
    } catch (IOException e) {
      Logger.error("Could not read profile file", e);
    }

    return null;
  }

  private String getEntry(String username) {
    try {
      List<String> userDb = Files.readAllLines(mUserFile.toPath());
      for (final ListIterator<String> i = userDb.listIterator(); i.hasNext(); ) {
        String entry = i.next();
        if (entry.startsWith(username)) {
          return entry;
        }
      }
    } catch (IOException e) {
      Logger.error("Could not read user file", e);
      return null;
    }
    return null;
  }

  private static String buildEntry(String username, String password) {
    return username.concat(":").concat(MD5Crypt.apacheCrypt(password));
  }

  private static String getEncryptedUsername(String email) {
    return DigestUtils.sha256Hex(email);
  }
}
