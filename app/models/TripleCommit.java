package models;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.riot.Lang;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Created by fo on 10.12.15, modified by pvb
 */

public class TripleCommit {

  private final Header mHeader;
  private final Diff mDiff;

  public static class Header {

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
        .concat(HEADER_SEPARATOR).concat(timestamp.toString()).concat("\n");
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

  public static class Diff {

    final private static String mLang = Lang.NTRIPLES.getName();

    final private List<Line> mLines;

    public static class Line {

      public final boolean add;
      public final Statement stmt;

      public Line(Statement stmt, boolean add) {
        this.add = add;
        this.stmt = stmt;
      }

    }

    public Diff() {
      mLines = new ArrayList<>();
    }

    public Diff(ArrayList<Line> aLineList) {
      mLines = aLineList;
    }

    public List<Line> getLines() {
      return this.mLines;
    }

    public void addStatement(Statement stmt) {
      this.mLines.add(new Line(stmt, true));
    }

    public void removeStatement(Statement stmt) {
      this.mLines.add(new Line(stmt, false));
    }

    public void apply(Model model) {

      for (Line line : this.mLines) {
        if (line.add) {
          model.add(line.stmt);
        } else {
          model.remove(line.stmt);
        }
      }

    }

    public void unapply(Model model) {

      for (Line line : this.mLines) {
        if (line.add) {
          model.remove(line.stmt);
        } else {
          model.add(line.stmt);
        }
      }

    }

    public String toString() {

      final Model buffer = ModelFactory.createDefaultModel();
      StringBuilder diffString = new StringBuilder();
      StringWriter triple = new StringWriter();

      for (Line line : this.mLines) {
        buffer.add(line.stmt).write(triple, mLang).removeAll();
        diffString.append((line.add ? "+ " : "- ").concat(triple.toString()));
        triple.getBuffer().setLength(0);
      }

      return diffString.toString();

    }

    public static Diff fromString(String aDiffString) {

      final Model buffer = ModelFactory.createDefaultModel();

      ArrayList<Line> lines = new ArrayList<>();

      Scanner scanner = new Scanner(aDiffString);
      String diffLine;
      while (scanner.hasNextLine()) {
        diffLine = scanner.nextLine().trim();
        if (diffLine.matches("^[+-] .*")) {
          buffer.read(new ByteArrayInputStream(diffLine.substring(1).getBytes(StandardCharsets.UTF_8)), null, mLang);
          lines.add(new Line(buffer.listStatements().nextStatement(), "+".equals(diffLine.substring(0, 1))));
          buffer.removeAll();
        } else {
          throw new IllegalArgumentException("Mal formed triple diff line: " + aDiffString);
        }
      }
      scanner.close();

      return new Diff(lines);

    }

  }

  public TripleCommit(Header aHeader, Diff aDiff) {
    mHeader = aHeader;
    mDiff = aDiff;
  }

  public Diff getDiff() {
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
