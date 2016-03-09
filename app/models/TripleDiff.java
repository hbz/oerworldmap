package models;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.jena.riot.Lang;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * Created by fo on 10.12.15, modified by pvb
 */

public class TripleDiff {

  final private List<Line> mLines = new ArrayList<>();
  final private static String mLang = Lang.NTRIPLES.getName();
  final private Model mBuffer = ModelFactory.createDefaultModel();
  private Header mHeader;

  public class Line {

    public final boolean add;
    public final Statement stmt;

    public Line(Statement stmt, boolean add) {
      this.add = add;
      this.stmt = stmt;
    }

  }

  public class Header {

    public final String author;
    public final ZonedDateTime timestamp;

    public Header(final String aAuthor, final ZonedDateTime aTimestamp) {
      this.author = aAuthor;
      this.timestamp = aTimestamp;
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("Author: ").append(author) //
          .append("\nDate: ").append(timestamp.toString()).append("\n\n");
      return sb.toString();
    }

    public String getAuthor() {
      return this.author;
    }

    public ZonedDateTime getTimestamp() {
      return this.timestamp;
    }
  }

  public List<Line> getLines() {
    return this.mLines;
  }

  public Header getHeader() {
    return this.mHeader;
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
    StringBuilder diffString = new StringBuilder();

    // write header
    diffString.append(mHeader.toString());

    // write triples
    StringWriter triple = new StringWriter();
    for (Line line : this.mLines) {
      mBuffer.add(line.stmt).write(triple, mLang).removeAll();
      diffString.append((line.add ? "+ " : "- ").concat(triple.toString()));
      triple.getBuffer().setLength(0);
    }
    return diffString.toString();
  }

  public void fromString(String aDiffString) {
    @SuppressWarnings("resource")
    Scanner scanner = new Scanner(aDiffString);
    String diffLine = null;

    // read header
    String author = null;
    ZonedDateTime timestamp = null;

    // read triples
    while (scanner.hasNextLine()) {
      diffLine = scanner.nextLine().trim();
      if (diffLine.matches("^[+-] .*")) {
        mBuffer.read(new ByteArrayInputStream(diffLine.substring(1).getBytes(StandardCharsets.UTF_8)), null, mLang);
        mLines.add(new Line(mBuffer.listStatements().nextStatement(), "+".equals(diffLine.substring(0, 1))));
        mBuffer.removeAll();
      } //
      else if (diffLine.startsWith("Author: ")) {
        author = diffLine.substring(8);
      } //
      else if (diffLine.startsWith("Date: ")) {
        timestamp = ZonedDateTime.parse(diffLine.substring(6));
      } //
      else if ("".equals(diffLine.trim())) {
        continue;
      } else {
        throw new IllegalArgumentException("Mal formed triple diff line: " + aDiffString);
      }
    }
    if (null != author && null != timestamp) {
      mHeader = new Header(author, timestamp);
    }
    scanner.close();
  }

}
