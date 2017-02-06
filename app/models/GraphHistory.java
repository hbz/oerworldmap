package models;

import org.apache.commons.io.FileUtils;
import org.apache.jena.atlas.RuntimeIOException;
import play.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by fo on 24.03.16.
 */
public class GraphHistory {

  private final File mCommitDir;
  private final File mHistoryFile;
  private final Map<String, List<Commit>> mIndex;
  private final List<Commit> mLog;


  public GraphHistory(File aCommitDir, File aHistoryFile) {

    if (!aCommitDir.isDirectory() || !aCommitDir.canWrite()) {
      throw new IllegalArgumentException("Not a writable directory: " + aCommitDir);
    }

    if (!aHistoryFile.isFile() || !aHistoryFile.canWrite()) {
      throw new IllegalArgumentException("Not a writable file: " + aHistoryFile);
    }

    mCommitDir = aCommitDir;
    mHistoryFile = aHistoryFile;
    mIndex = new HashMap<>();
    mLog = new ArrayList<>();

    for (Commit commit : this.fetch()) {
      indexCommit(commit);
    }

  }

  public void add(Commit aCommit) throws IOException {

    String commitId = aCommit.getId();
    File commitFile = new File(mCommitDir, commitId);
    FileUtils.writeStringToFile(commitFile, aCommit.toString(), StandardCharsets.UTF_8);
    FileUtils.writeStringToFile(mHistoryFile, commitId.concat("\n"), StandardCharsets.UTF_8, true);
    indexCommit(aCommit);

  }

  public int size() {

    return mLog.size();

  }

  public List<Commit> log() {

    return mLog;

  }

  public List<Commit> log(String aURI) {

    if (!mIndex.containsKey(aURI)) {
      return new ArrayList<>();
    }

    return mIndex.get(aURI);

  }

  public List<Commit> until(String aCommitId) {

    List<Commit> commits = new ArrayList<>();

    for (Commit commit : log()) {
      if (commit.getId().equals(aCommitId)) {
        break;
      }
      commits.add(commit);
    }

    return commits;

  }

  private List<Commit> fetch() {

    List<String> commitIds;

    try {
      commitIds = FileUtils.readLines(mHistoryFile, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeIOException(e);
    }

    List<Commit> commits = new ArrayList<>();

    for (String commitId : commitIds) {
      File commitFile = new File(mCommitDir, commitId);
      try {
        TripleCommit commit = TripleCommit.fromString(FileUtils.readFileToString(commitFile, StandardCharsets.UTF_8));
        commits.add(commit);
      } catch (IllegalArgumentException | IOException e) {
        Logger.error("Could not read commit, skipping", e);
      }
    }

    return commits;

  }

  private Set<String> getModified(Commit aCommit) {
    Set<String> modified = new HashSet<>();
    for (Commit.Diff.Line line : aCommit.getDiff().getLines()) {
      org.apache.jena.rdf.model.Resource subject = ((TripleCommit.Diff.Line) line).stmt.getSubject();
      org.apache.jena.rdf.model.RDFNode object = ((TripleCommit.Diff.Line) line).stmt.getObject();
      if (subject.isURIResource()) {
        modified.add(subject.toString());
      }
      if (object.isURIResource()) {
        modified.add(object.toString());
      }
    }
    return modified;
  }

  private void indexCommit(Commit aCommit) {
    mLog.add(0, aCommit);
    for (String id : getModified(aCommit)) {
      if (!mIndex.containsKey(id)) {
        mIndex.put(id, new ArrayList<>());
      }
      mIndex.get(id).add(0, aCommit);
    }
  }

}
