package models;

import models.TripleCommit.Diff.Line;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.jena.atlas.RuntimeIOException;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.RDF;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Created by fo on 10.12.15, modified by pvb
 */

public class TripleCommit implements Commit {

  private final Header mHeader;
  private final Commit.Diff mDiff;

  public static class Header implements Commit.Header {

    public static final String AUTHOR_HEADER = "Author";
    public static final String DATE_HEADER = "Date";
    static final String PRIMARY_TOPIC_HEADER = "Primary topic";
    static final String MIGRATION_FLAG_HEADER = "Is migration";
    static final String MIGRATION_FLAG = "true";
    static final String HEADER_SEPARATOR = ": ";

    private final String author;
    private final ZonedDateTime timestamp;
    private final String primaryTopic;
    private final boolean isMigration;

    public Header(final String aAuthor, final ZonedDateTime aTimestamp) {
      this(aAuthor, aTimestamp, null, false);
    }

    public Header(final String aAuthor, final ZonedDateTime aTimestamp, final String aPrimaryTopic) {
      this(aAuthor, aTimestamp, aPrimaryTopic, false);
    }

    public Header(final String aAuthor, final ZonedDateTime aTimestamp, final boolean aIsMigration) {
      this(aAuthor, aTimestamp, null, aIsMigration);
    }

    public Header(final String aAuthor, final ZonedDateTime aTimestamp, final String aPrimaryTopic,
                  final boolean aIsMigration) {
      this.author = aAuthor;
      this.timestamp = aTimestamp;
      this.primaryTopic = aPrimaryTopic;
      this.isMigration = aIsMigration;
    }

    public String toString() {
      String _timestamp =  timestamp.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
      String commit = AUTHOR_HEADER.concat(HEADER_SEPARATOR).concat(author).concat("\n")
        .concat(DATE_HEADER).concat(HEADER_SEPARATOR).concat(_timestamp).concat("\n");
      if (primaryTopic != null) {
        commit += PRIMARY_TOPIC_HEADER.concat(HEADER_SEPARATOR).concat(primaryTopic).concat("\n");
      }
      if (isMigration) {
        commit += MIGRATION_FLAG_HEADER.concat(HEADER_SEPARATOR).concat(MIGRATION_FLAG).concat("\n");
      }
      return commit;
    }

    public String getAuthor() {
      return this.author;
    }

    public ZonedDateTime getTimestamp() {
      return this.timestamp;
    }

    public String getPrimaryTopic() {
      return this.primaryTopic;
    }

    public boolean isMigration() {
      return this.isMigration;
    }

    static Header fromString(String aHeaderString) {
      Scanner scanner = new Scanner(aHeaderString);
      String author = null;
      ZonedDateTime timestamp = null;
      String primaryTopic = null;
      boolean isMigration = false;

      while(scanner.hasNextLine()) {
        String headerLine = scanner.nextLine();
        String[] header = headerLine.split(":", 2);
        String headerName = header[0];
        String headerValue = header[1].trim();
        switch (headerName) {
          case AUTHOR_HEADER:
            author = headerValue;
            break;
          case DATE_HEADER:
            timestamp = ZonedDateTime.parse(headerValue);
            break;
          case PRIMARY_TOPIC_HEADER:
            primaryTopic = headerValue;
            break;
          case MIGRATION_FLAG_HEADER:
            isMigration = headerValue.equals(MIGRATION_FLAG);
            break;
          default:
            throw new IllegalArgumentException("Invalid header");
        }
      }

      if (author == null || timestamp == null) {
        throw new IllegalArgumentException("Invalid commit");
      }

      return new Header(author, timestamp, primaryTopic, isMigration);
    }
  }

  public static class Diff implements Commit.Diff {

    final private static String mLang = Lang.NTRIPLES.getName();
    final private List<Commit.Diff.Line> mLines;

    public static class Line extends Commit.Diff.Line {
      //public final boolean add;
      public final Statement stmt;

      Line(Statement stmt, boolean add) {
        this.add = add;
        this.stmt = stmt;
      }
    }

    public Diff() {
      mLines = new ArrayList<>();
    }

    public Diff(ArrayList<Commit.Diff.Line> aLineList) {
      mLines = aLineList;
    }

    public List<Commit.Diff.Line> getLines() {
      return this.mLines;
    }

    public void addStatement(Statement stmt) {
      this.mLines.add(new Line(stmt, true));
    }

    public void removeStatement(Statement stmt) {
      this.mLines.add(new Line(stmt, false));
    }

    @Override
    public void apply(Object model) {
      apply((Model) model);
    }

    public void apply(Model model) {
      for (Commit.Diff.Line line : this.mLines) {
        if (line.add) {
          model.add(((Line) line).stmt);
        } else {
          model.remove(((Line) line).stmt);
        }
      }
    }

    @Override
    public void append(Commit.Diff diff) {
      mLines.addAll(diff.getLines());
    }

    @Override
    public void unapply(Object model) {
      unapply((Model) model);
    }

    void unapply(Model model) {
      for (Commit.Diff.Line line : this.mLines) {
        if (line.add) {
          model.remove(((Line) line).stmt);
        } else {
          model.add(((Line) line).stmt);
        }
      }
    }

    public Diff reverse() {
      TripleCommit.Diff reverse = new TripleCommit.Diff();
      for (Commit.Diff.Line line : mLines) {
        if (line.add) {
          reverse.removeStatement(((Line) line).stmt);
        } else {
          reverse.addStatement(((Line) line).stmt);
        }
      }
      return reverse;
    }

    public String toString() {
      final Model buffer = ModelFactory.createDefaultModel();
      StringBuilder diffString = new StringBuilder();
      StringWriter triple = new StringWriter();

      for (Commit.Diff.Line line : this.mLines) {
        Statement statement = ((Line) line).stmt;
        Resource subject = statement.getSubject();
        Property predicate = statement.getPredicate();
        RDFNode object = statement.getObject();
        if (subject.isAnon()) {
          subject = ResourceFactory.createResource("_:".concat(subject.toString()));
        }
        if (object.isAnon()) {
          object = ResourceFactory.createResource("_:".concat(object.toString()));
        }
        Statement skolemized = ResourceFactory.createStatement(subject, predicate, object);
        buffer.add(skolemized).write(triple, mLang).removeAll();
        diffString.append((((Line) line).add ? "+ " : "- ").concat(triple.toString()));
        triple.getBuffer().setLength(0);
      }
      return diffString.toString();
    }

    public static Diff fromString(String aDiffString) {
      final Model buffer = ModelFactory.createDefaultModel();
      ArrayList<Commit.Diff.Line> lines = new ArrayList<>();
      Scanner scanner = new Scanner(aDiffString).useDelimiter("\\n");
      String diffLine;
      while (scanner.hasNext()) {
        diffLine = scanner.next().trim();
        String op = diffLine.substring(0, 1);
        if (!op.equals("+") && !op.equals("-")) {
          throw new IllegalArgumentException("Diff Line malformed: " + diffLine);
        }
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
          diffLine.substring(1)
            .getBytes(StandardCharsets.UTF_8))) {
          buffer.read(byteArrayInputStream, null, mLang);
          lines.add(new Line(buffer.listStatements().nextStatement(), op.equals("+")));
          buffer.removeAll();
        } catch (IOException e) {
          throw new RuntimeIOException(e);
        }
      }
      scanner.close();
      return new Diff(lines);
    }
  }

  private static class Properties {
    static String eccrev = "https://vocab.eccenca.com/revision/";
    static Property previousCommit = ResourceFactory.createProperty(eccrev, "previousCommit");
    static Property commitAuthor = ResourceFactory.createProperty(eccrev, "commitAuthor");
    static Property atTime = ResourceFactory.createProperty(eccrev, "atTime");
    static Property hasRevision = ResourceFactory.createProperty(eccrev, "hasRevision");
    static Property deltaInsert = ResourceFactory.createProperty(eccrev, "deltaInsert");
    static Property deltaDelete = ResourceFactory.createProperty(eccrev, "deltaDelete");
  }

  public TripleCommit(Header aHeader, Commit.Diff aDiff) {
    mHeader = aHeader;
    mDiff = aDiff;
  }

  public Commit.Diff getDiff() {
    return this.mDiff;
  }

  public Header getHeader() {
    return this.mHeader;
  }

  public String getId() {
    return DigestUtils.sha1Hex(this.toString());
  }

  public String toString() {
    return mHeader.toString().concat("\n").concat(mDiff.toString());
  }

  public boolean equals(Object aOther) {
    return aOther instanceof TripleCommit && this.getId().equals(((TripleCommit) aOther).getId());
  }

  public static TripleCommit fromString(String aCommitString) {
    String[] parts = aCommitString.split("\\n\\n");
    if (!(parts.length == 2)) {
      throw new IllegalArgumentException("Malformed commit");
    }
    return new TripleCommit(Header.fromString(parts[0]), Diff.fromString(parts[1]));
  }

  private Model getInsertions() {
    Model deltaInsert = ModelFactory.createDefaultModel();
    for (Commit.Diff.Line line : this.getDiff().getLines()) {
      Line diffLine = ((Line) line);
      if (diffLine.add) {
        deltaInsert.add(diffLine.stmt);
      }
    }
    return deltaInsert;
  }

  private Model getDeletions() {
    Model deltaDelete = ModelFactory.createDefaultModel();
    for (Commit.Diff.Line line : this.getDiff().getLines()) {
      Line diffLine = ((Line) line);
      if (!diffLine.add) {
        deltaDelete.add(diffLine.stmt);
      }
    }
    return deltaDelete;
  }

  public Resource getPrimaryTopic() {
    if (this.getHeader().getPrimaryTopic() != null) {
      return ResourceFactory.createResource(this.getHeader().getPrimaryTopic());
    }
    try {
      return getInsertions().listResourcesWithProperty(RDF.type).nextResource();
    } catch (NoSuchElementException e) {
      for (Commit.Diff.Line line : this.getDiff().getLines()) {
        Resource subject = ((Line) line).stmt.getSubject();
        if (subject.isURIResource()) {
          return subject;
        }
      }
      return ((Line) this.getDiff().getLines().get(0)).stmt.getSubject();
    }
  }

  public Dataset toRDF() {
    Model commit = ModelFactory.createDefaultModel();
    Resource deltaInsertGraph = commit.createResource(this.getId() + "#insert");
    Resource deltaDeleteGraph = commit.createResource(this.getId() + "#delete");
    Dataset changes = DatasetFactory.create(commit);
    final Model deltaInsert = changes.getNamedModel(deltaInsertGraph.getURI());
    final Model deltaDelete = changes.getNamedModel(deltaDeleteGraph.getURI());
    deltaInsert.add(this.getInsertions());
    deltaDelete.add(this.getDeletions());
    final Resource revision = commit.createResource()
      .addProperty(Properties.deltaInsert, deltaInsertGraph)
      .addProperty(Properties.deltaDelete, deltaDeleteGraph);
    final Commit.Header header = this.getHeader();
    commit.createResource(this.getId())
      .addProperty(Properties.commitAuthor, commit.createResource(header.getAuthor()))
      .addProperty(Properties.atTime, header.getTimestamp().toString())
      .addProperty(Properties.hasRevision, revision);
    return changes;
  }

}
