package services;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;

import com.typesafe.config.ConfigFactory;
import helpers.MD5Crypt;
import helpers.UniversalFunctions;
import models.Resource;

import org.apache.commons.codec.digest.DigestUtils;

import org.apache.commons.io.FileUtils;
import play.Configuration;
import play.Logger;
import controllers.Global;

/**
 * @author fo
 */
public class Account {

  private static final String mTokenDir = Global.getConfig().getString("user.token.dir");
  private static final String htpasswd = Global.getConfig().getString("ht.passwd");
  private static final File userFile = new File(htpasswd);
  private static final SecureRandom mRandom = new SecureRandom();

  public static String createTokenFor(Resource user) {

    if (! Arrays.asList(Global.getConfig().getString("users.valid").split(" +"))
      .contains(user.get("email").toString())) {
      Logger.warn("Token for invalid user ".concat(user.getAsString("email").concat(" requested.")));
      return null;
    }

    String token = new BigInteger(130, mRandom).toString(32);
    String username = user.getAsString("email");
    String entry = username.concat(":").concat(MD5Crypt.apacheCrypt(token));

    try {
      List<String> userDb = Files.readAllLines(userFile.toPath());
      boolean present = false;
      for (final ListIterator<String> i = userDb.listIterator(); i.hasNext();) {
        if (i.next().startsWith(username)) {
          i.set(entry);
          present = true;
          break;
        }
      }
      if (!present) {
        userDb.add(entry);
      }
      FileUtils.writeLines(userFile,userDb);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return token;

  }

  public static void removeTokenFor(Resource user) {
    String username = user.getAsString("email");
    try {
      List<String> userDb = Files.readAllLines(userFile.toPath());
      for (final ListIterator<String> i = userDb.listIterator(); i.hasNext();) {
        if (i.next().startsWith(username)) {
          i.remove();
          break;
        }
      }
      FileUtils.writeLines(userFile,userDb);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static boolean authenticate(String username, String password) {

    if (username.equals(Global.getConfig().getString("admin.user"))
        && password.equals(Global.getConfig().getString("admin.pass"))) {
      return true;
    }

    Path tokenFile = Paths.get(mTokenDir, getEncryptedEmailAddress(username));
    try {
      if (new String(Files.readAllBytes(tokenFile)).equals(password)) {
        return true;
      }
    } catch (IOException e) {
      return false;
    }

    return false;

  }

  public static String getEncryptedEmailAddress(Resource user) {
    return DigestUtils.sha256Hex(user.get("email").toString());
  }

  public static String getEncryptedEmailAddress(String email) {
    return DigestUtils.sha256Hex(email);
  }

}
