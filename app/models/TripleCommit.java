package models;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.atlas.RuntimeIOException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public static final String HEADER_SEPARATOR = ": ";

    public final String author;
    public final ZonedDateTime timestamp;

    public Header(final String aAuthor, final ZonedDateTime aTimestamp) {
      this.author = aAuthor;
      this.timestamp = aTimestamp;
    }

    public String toString() {
      return AUTHOR_HEADER.concat(HEADER_SEPARATOR).concat(author).concat("\n").concat(DATE_HEADER)
        .concat(HEADER_SEPARATOR).concat(timestamp.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)).concat("\n");
    }

    public Map<String, String> toMap() {
      Map<String, String> map = new HashMap<>();
      map.put(AUTHOR_HEADER, author);
      map.put(DATE_HEADER, timestamp.toString());
      return map;
    }

    public String getAuthor() {
      return this.author;
    }

    public ZonedDateTime getTimestamp() {
      return this.timestamp;
    }

    public static Header fromString(String aHeaderString) {

      Scanner scanner = new Scanner(aHeaderString);

      String authorHeader;
      try {
        authorHeader = scanner.nextLine();
      } catch (NoSuchElementException e) {
        throw new IllegalArgumentException("Header missing author line");
      }
      String author = authorHeader.substring(8);
      if (!authorHeader.startsWith("Author: ") || StringUtils.isEmpty(author)) {
        throw new IllegalArgumentException("Header missing author");
      }

      String timestampHeader;
      try {
        timestampHeader = scanner.nextLine();
      } catch (NoSuchElementException e) {
        throw new IllegalArgumentException("Header missing author line");
      }

      ZonedDateTime timestamp = null;
      try {
        timestamp = ZonedDateTime.parse(timestampHeader.substring(6));
        if (!timestampHeader.startsWith("Date: ")) {
          throw new IllegalArgumentException("Header missing date line");
        }
      } catch (DateTimeParseException e) {
        throw new IllegalArgumentException("Header contains invalid date");
      }

      return new Header(author, timestamp);

    }

  }

  public static class Diff implements Commit.Diff {

    final private static String mLang = Lang.NTRIPLES.getName();

    final private List<Commit.Diff.Line> mLines;

    public static class Line extends Commit.Diff.Line {

      //public final boolean add;
      public final Statement stmt;

      public Line(Statement stmt, boolean add) {
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

    public void unapply(Model model) {

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
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(diffLine.substring(1)
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

  public String toString() {
    return mHeader.toString().concat("\n").concat(mDiff.toString());
  }

  public boolean equals(Object aOther) {

    return aOther instanceof TripleCommit
      && DigestUtils.sha1Hex(this.toString()).equals(DigestUtils.sha1Hex(aOther.toString()));

  }

  public static TripleCommit fromString(String aCommitString) {

    String[] parts = aCommitString.split("\\n\\n");
    if (!(parts.length == 2)) {
      throw new IllegalArgumentException("Malformed commit");
    }
    return new TripleCommit(Header.fromString(parts[0]), Diff.fromString(parts[1]));

  }

}
