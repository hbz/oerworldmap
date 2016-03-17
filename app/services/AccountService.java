package services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.ListIterator;

import helpers.MD5Crypt;

import org.apache.commons.codec.digest.DigestUtils;

import org.apache.commons.io.FileUtils;
import play.Logger;

/**
 * @author fo
 */
public class AccountService {

  private final File mTokenDir;
  private final File mUserFile;

  public AccountService(File aTokenDir, File aUserFile) {

    mTokenDir = aTokenDir;
    mUserFile = aUserFile;

  }

  public String addUser(String username, String password) {

    if (!userExists(username) && !pendingVerification(username)) {
      try {
        String token = getEncryptedUsername(username);
        File tokenFile = new File(mTokenDir, token);
        FileUtils.writeStringToFile(tokenFile, buildEntry(username, password));
        return token;
      } catch (IOException e) {
        Logger.error(e.toString());
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
      for (final ListIterator<String> i = userDb.listIterator(); i.hasNext();) {
        if (i.next().startsWith(username)) {
          i.remove();
          break;
        }
      }
      FileUtils.writeLines(mUserFile,userDb);
      return true;
    } catch (IOException e) {
      Logger.error(e.toString());
      return false;
    }

  }

  public String verifyToken(String token) {

    File tokenFile = new File(mTokenDir, token);

    if (tokenFile.exists()) {
      try {
        String entry = FileUtils.readFileToString(tokenFile);
        new BufferedWriter(new FileWriter(mUserFile, true)).append(entry.concat("\n")).close();
        FileUtils.forceDelete(tokenFile);
        return entry.split(":")[0];
      } catch (IOException e) {
        Logger.error(e.toString());
      }
    }

    return null;

  }

  public boolean userExists(String username) {

    try {
      return FileUtils.readFileToString(mUserFile).contains(username);
    } catch (IOException e) {
      Logger.error(e.toString());
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
      for (final ListIterator<String> i = userDb.listIterator(); i.hasNext();) {
        if (i.next().startsWith(username)) {
          i.set(buildEntry(username, password));
          break;
        }
      }
      FileUtils.writeLines(mUserFile,userDb);
      return true;
    } catch (IOException e) {
      Logger.error(e.toString());
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

  private String getEntry(String username) {
    try {
      List<String> userDb = Files.readAllLines(mUserFile.toPath());
      for (final ListIterator<String> i = userDb.listIterator(); i.hasNext();) {
        String entry = i.next();
        if (entry.startsWith(username)) {
          return entry;
        }
      }
    } catch (IOException e) {
      Logger.error(e.toString());
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
