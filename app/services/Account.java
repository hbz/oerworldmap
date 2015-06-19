package services;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;

import models.Resource;

import org.apache.commons.codec.digest.DigestUtils;

import play.Play;

/**
 * @author fo
 */
public class Account {

  private static final String mTokenDir = Play.application().configuration().getString("user.token.dir");
  private static final SecureRandom mRandom = new SecureRandom();

  public static String createTokenFor(Resource user) {
    String token = new BigInteger(130, mRandom).toString(32);
    Path tokenFile = Paths.get(mTokenDir, getEncryptedEmailAddress(user));
    try {
      Files.write(tokenFile, token.getBytes());
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
    return token;
  }

  public static void removeTokenFor(Resource user) {
    Path tokenFile = Paths.get(mTokenDir, getEncryptedEmailAddress(user));
    try {
      Files.delete(tokenFile);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static boolean authenticate(String username, String password) {
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
