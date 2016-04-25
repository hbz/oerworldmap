package models;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.jena.atlas.RuntimeIOException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by fo on 24.03.16.
 */
public class GraphHistory {

  private final File mCommitDir;
  private final File mHistoryFile;

  public GraphHistory(File aCommitDir, File aHistoryFile) {

    if (!aCommitDir.isDirectory() || !aCommitDir.canWrite()) {
      throw new IllegalArgumentException("Not a writable directory: " + aCommitDir);
    }

    if (!aHistoryFile.isFile() || !aHistoryFile.canWrite()) {
      throw new IllegalArgumentException("Not a writable file: " + aHistoryFile);
    }

    mCommitDir = aCommitDir;
    mHistoryFile = aHistoryFile;

  }

  public void add(Commit aCommit) throws IOException {

    String commitId = DigestUtils.sha1Hex(aCommit.toString());
    File commitFile = new File(mCommitDir, commitId);
    FileUtils.writeStringToFile(commitFile, aCommit.toString());
    FileUtils.writeStringToFile(mHistoryFile, commitId.concat("\n"), true);

  }

  public int size() {

    try (InputStream in = new BufferedInputStream(new FileInputStream(mHistoryFile))) {
      byte[] buf = new byte[4096 * 16];
      int c;
      int lineCount = 0;
      while ((c = in.read(buf)) > 0) {
        for (int i = 0; i < c; i++) {
          if (buf[i] == '\n') lineCount++;
        }
      }
      return lineCount;
    } catch (IOException e) {
      throw new RuntimeIOException(e);
    }

  }

  public List<Commit> log() {

    List<String> commitIds;

    try {
      commitIds = FileUtils.readLines(mHistoryFile);
    } catch (IOException e) {
      throw new RuntimeIOException(e);
    }

    List<Commit> commits = new ArrayList<>();

    for (String commitId : commitIds) {
      File commitFile = new File(mCommitDir, commitId);
      try {
        TripleCommit commit = TripleCommit.fromString(FileUtils.readFileToString(commitFile));
        commits.add(commit);
      } catch (IOException e) {
        throw new RuntimeIOException(e);
      }
    }

    return commits;

  }

  public List<Commit> log(String aURI) {

    List<String> commitIds;

    try {
      commitIds = FileUtils.readLines(mHistoryFile);
      //Collections.reverse(commitIds);
    } catch (IOException e) {
      throw new RuntimeIOException(e);
    }

    List<Commit> commits = new ArrayList<>();

    String regex = "^[+-] <".concat(aURI).concat(">|<").concat(aURI).concat("> \\.$");
    Pattern p = Pattern.compile(regex, Pattern.MULTILINE);
    for (String commitId : commitIds) {
      File commitFile = new File(mCommitDir, commitId);
      try {
        String commitString = FileUtils.readFileToString(commitFile);
        if (p.matcher(commitString).find()) {
          TripleCommit commit = TripleCommit.fromString(commitString);
          commits.add(commit);
        }
      } catch (IOException e) {
        throw new RuntimeIOException(e);
      }
    }

    return commits;

  }

}
