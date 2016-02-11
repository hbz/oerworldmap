package models;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
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
  final private String mLang = Lang.NTRIPLES.getName();
  final private Model mBuffer = ModelFactory.createDefaultModel();

  public class Line {

    public final boolean add;
    public final Statement stmt;

    public Line(Statement stmt, boolean add) {
      this.add = add;
      this.stmt = stmt;
    }

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
    String diffString = "";
    StringWriter triple = new StringWriter();
    for (Line line : this.mLines) {
      mBuffer.add(line.stmt).write(triple, mLang).removeAll();
      diffString += (line.add ? "+ " : "- ").concat(triple.toString());
      triple.getBuffer().setLength(0);
    }
    return diffString;
  }

  public void fromString(String aDiffString) {
    Scanner scanner = new Scanner(aDiffString);
    while (scanner.hasNextLine()) {
      String diffLine = scanner.nextLine().trim();
      mBuffer.read(new ByteArrayInputStream(diffLine.substring(1).getBytes(StandardCharsets.UTF_8)), null, mLang);
      mLines.add(new Line(mBuffer.listStatements().nextStatement(), "+".equals(diffLine.substring(0, 1))));
      mBuffer.removeAll();
    }
    scanner.close();
  }


}
