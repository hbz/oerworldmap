package models;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

/**
 * Created by fo on 10.12.15, modified by pvb
 */

public class TripleDiff {

  final private List<Line> mLines = new ArrayList<>();
  final private static String PATTERN = " *[+-]( +([!#-ƒ]+|\"[ -ƒ]+\")){3}";

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
    Model model = ModelFactory.createDefaultModel();
    for (Line line : this.mLines) {
      StringWriter triple = new StringWriter();
      RDFDataMgr.write(triple, model.add(line.stmt), Lang.NTRIPLES);
      model.removeAll();
      if (line.add) {
        diffString += "+ ".concat(triple.toString());
      } else {
        diffString += "- ".concat(triple.toString());
      }
    }
    return diffString;
  }

  public void fromString(String aDiffString) {
    Scanner scanner = new Scanner(aDiffString);
    Model model = ModelFactory.createDefaultModel();
    while (scanner.hasNextLine()) {
      addLine(scanner.nextLine(), model);
      model.removeAll();
    }
    scanner.close();
  }

  private void addLine(final String aDiffLine, final Model aEmptyModel) {

    // prepare
    String diffline = aDiffLine.trim();

    // check aDiffLine is well formed
    if (!aDiffLine.matches(PATTERN)) {
      throw new IllegalArgumentException("Diff Line malformatted: " + aDiffLine);
    }

    // read operator
    boolean add;
    if ("+".equals(diffline.charAt(0))) {
      add = true;
    } else {
      add = false;
    }

    // read statement
    RDFDataMgr.read(aEmptyModel,
        new ByteArrayInputStream(diffline.substring(1).getBytes(StandardCharsets.UTF_8)),
        Lang.NTRIPLES);
    StmtIterator it = aEmptyModel.listStatements();
    Statement statement = it.nextStatement();

    // finally, add the line
    mLines.add(new Line(statement, add));
  }

}
