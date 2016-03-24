package models;

import static org.junit.Assert.*;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Created by fo on 24.03.16.
 */
public class GraphHistoryTest {

  private File mHistoryDir;
  private File mHistoryFile;

  private GraphHistory mGraphHistory;

  private static String loadCommit(String aFileName) throws IOException {

    return IOUtils.toString(
      ClassLoader.getSystemResourceAsStream(aFileName), StandardCharsets.UTF_8.name());

  }

  @Before
  public void setUp() throws IOException {

    mHistoryDir = Files.createTempDirectory(null).toFile();
    mHistoryFile = Files.createTempFile(null, null).toFile();
    mGraphHistory = new GraphHistory(mHistoryDir, mHistoryFile);

  }


  @Test
  public void testAdd() throws IOException {

    TripleCommit commit = TripleCommit.fromString(loadCommit("GraphHistoryTest/testAddCommit.IN.1.ncommit"));
    mGraphHistory.add(commit);
    String commitId = DigestUtils.sha1Hex(commit.toString());
    File expectedFile = new File(mHistoryDir, commitId);
    assertTrue(expectedFile.exists());
    assertEquals(FileUtils.readFileToString(mHistoryFile), commitId.concat("\n"));

  }

  @Test
  public void testSize() throws IOException {

    TripleCommit commit = TripleCommit.fromString(loadCommit("GraphHistoryTest/testAddCommit.IN.1.ncommit"));
    mGraphHistory.add(commit);
    assertEquals(1, mGraphHistory.size());

  }

  @Test
  public void testFullLog() throws IOException {

    TripleCommit commit1 = TripleCommit.fromString(loadCommit("GraphHistoryTest/testAddCommit.IN.1.ncommit"));
    TripleCommit commit2 = TripleCommit.fromString(loadCommit("GraphHistoryTest/testAddCommit.IN.2.ncommit"));
    mGraphHistory.add(commit1);
    mGraphHistory.add(commit2);
    List<TripleCommit> commits = mGraphHistory.log();
    assertEquals(2, commits.size());
    assertEquals(commit1, commits.get(0));
    assertEquals(commit2, commits.get(1));

  }

  @Test
  public void testLog() throws IOException {

    TripleCommit commit1 = TripleCommit.fromString(loadCommit("GraphHistoryTest/testAddCommit.IN.1.ncommit"));
    TripleCommit commit2 = TripleCommit.fromString(loadCommit("GraphHistoryTest/testAddCommit.IN.2.ncommit"));
    mGraphHistory.add(commit1);
    mGraphHistory.add(commit2);
    List<TripleCommit> resource456Commits = mGraphHistory.log("info:456");
    assertEquals(2, resource456Commits.size());
    assertEquals(commit1, resource456Commits.get(0));
    assertEquals(commit2, resource456Commits.get(1));
    List<TripleCommit> resource123Commits = mGraphHistory.log("info:123");
    assertEquals(1, resource123Commits.size());
    assertEquals(commit2, resource123Commits.get(0));
    List<TripleCommit> resource789Commits = mGraphHistory.log("info:789");
    assertEquals(0, resource789Commits.size());

  }

}
