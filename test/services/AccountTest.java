package services;

import static org.junit.Assert.*;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Created by fo on 17.03.16.
 */
public class AccountTest {

  private final String mTestUsername = "username";
  private final String mTestPassword = "password";
  private final String mTestToken = DigestUtils.sha256Hex(mTestUsername);

  private File mTokenDir;
  private File mUserFile;
  private AccountService mAccountService;

  @Before
  public void setUp() throws IOException {

    mTokenDir = Files.createTempDirectory(null).toFile();
    mUserFile = Files.createTempFile(null, null).toFile();
    mAccountService = new AccountService(mTokenDir, mUserFile);

  }

  @After
  public void tearDown() {

    mTokenDir.delete();
    mUserFile.delete();

  }


  @Test
  public void testAddUser() {

    String token = mAccountService.addUser(mTestUsername, mTestPassword);
    assertNotNull(token);
    File tokenFile = new File(mTokenDir, token);
    assertTrue(tokenFile.exists());

  }

  @Test
  public void testVerfifyUser() throws IOException {

    String entry = mTestUsername.concat(":").concat(mTestPassword);
    File tokenFile = new File(mTokenDir, mTestToken);
    FileUtils.writeStringToFile(tokenFile, entry);
    String user = mAccountService.verifyToken(mTestToken);
    assertEquals(mTestUsername, user);
    assertFalse(tokenFile.exists());

  }

  @Test
  public void testUserExists() throws IOException {

    String entry = mTestUsername.concat(":").concat(mTestPassword);
    FileUtils.writeStringToFile(mUserFile, entry);
    assertTrue(mAccountService.userExists(mTestUsername));

  }

  @Test
  public void testPendingVerification() throws IOException {

    File tokenFile = new File(mTokenDir, mTestToken);
    assertTrue(tokenFile.createNewFile());
    assertTrue(mAccountService.pendingVerification(mTestUsername));

  }

  @Test
  public void deleteUser() throws IOException {

    String entry = mTestUsername.concat(":").concat(mTestPassword);
    FileUtils.writeStringToFile(mUserFile, entry);
    mAccountService.deleteUser(mTestUsername);
    assertFalse(FileUtils.readFileToString(mUserFile).contains(entry));

  }

  @Test
  public void testValidatePassword() throws IOException {

    mAccountService.verifyToken(mAccountService.addUser(mTestUsername, mTestPassword));
    assertTrue(mAccountService.validatePassword(mTestUsername, mTestPassword));

  }

  @Test
  public void testUpdatePassword() throws IOException {

    String updated = "foo";
    mAccountService.verifyToken(mAccountService.addUser(mTestUsername, mTestPassword));
    assertTrue(mAccountService.validatePassword(mTestUsername, mTestPassword));
    assertTrue(mAccountService.updatePassword(mTestUsername, mTestPassword, updated));
    assertTrue(mAccountService.validatePassword(mTestUsername, updated));

  }

}
