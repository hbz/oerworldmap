package models;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotException;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.ZonedDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by fo on 11.02.16.
 */
public class TripleCommitTest {

  // @Test
  public void testPlainLiteralLine() {
    TripleCommit.Diff.fromString(
        " + <http://example.org/show/218> <http://www.w3.org/2000/01/rdf-schema#label> \"That Seventies Show\" .");
  }

  // @Test
  public void testAddLanguageLiteralLine() {
    TripleCommit.Diff
        .fromString("+ <http://example.org/show/218> <http://example.org/show/localName> \"That Seventies Show\"@en .");
  }

  // @Test
  public void testAddTypedLiteralLine() {
    TripleCommit.Diff.fromString(
      "+ <http://en.wikipedia.org/wiki/Helium> <http://example.org/elements/specificGravity> \"1.663E-4\"^^<http://www.w3.org/2001/XMLSchema#double> .");
  }

  // @Test(expected = RiotException.class)
  public void testInvalidLiteral() {
    TripleCommit.Diff.fromString(
        " + <http://example.org/show/218> <http://www.w3.org/2000/01/rdf-schema#label> \"That Seventies Show .");
  }

  // @Test(expected = IllegalArgumentException.class)
  public void testInvalidOp() {
    TripleCommit.Diff.fromString(
        " | <http://example.org/show/218> <http://www.w3.org/2000/01/rdf-schema#label> \"That Seventies Show\" .");
  }

  // @Test
  public void testReadHeader() {

    TripleCommit.Header header = TripleCommit.Header.fromString( //
      "Author: unittest@oerworldmap.org\n" //
        + "Date: " + ZonedDateTime.now().toString() + "\n");
    Assert.assertNotNull(header);

  }

  // @Test(expected = IllegalArgumentException.class)
  public void testMissingAuthorHeader() {
    TripleCommit.Header.fromString("Date: " + ZonedDateTime.now().toString() + "\n");
  }

  // @Test(expected = IllegalArgumentException.class)
  public void testMissingDateHeader() {
    TripleCommit.Header.fromString("Author: Foo Bar <foo@bar.de>");
  }

  // @Test
  public void testValidCommit() {
    TripleCommit commit = TripleCommit.fromString(
      "Author: Foo Bar <foo@bar.de>\n" +
        "Date: 2007-12-03T10:15:30+01:00\n" +
        "\n" +
        "+ <urn:uuid:foo> <urn:uuid:bar> <urn:uuid:baz> .");
    assertNotNull(commit);
  }

  // @Test(expected = IllegalArgumentException.class)
  public void testInvalidCommit() {
    TripleCommit commit = TripleCommit.fromString(
      "Author: Foo Bar <foo@bar.de>\n" +
        "Date: 2007-12-03T10:15:30+01:00\n" +
        "+ <urn:uuid:foo> <urn:uuid:bar> <urn:uuid:baz> .");
    assertNotNull(commit);
  }

  // @Test
  public void testApplyDiff() {
    String ntriple = "<info:subject> <info:predicate> <info:object> .";
    Model actual = ModelFactory.createDefaultModel();
    TripleCommit.Diff diff = TripleCommit.Diff.fromString("+ ".concat(ntriple));
    diff.apply(actual);
    Model expected = ModelFactory.createDefaultModel();
    expected.read(new ByteArrayInputStream(ntriple.getBytes()), null, Lang.NTRIPLES.getName());
    assertTrue(expected.isIsomorphicWith(actual));
  }

  // @Test
  public void testUnapplyDiff() {
    String ntriple = "<info:subject> <info:predicate> <info:object> .";
    Model actual = ModelFactory.createDefaultModel();
    actual.read(new ByteArrayInputStream(ntriple.getBytes()), null, Lang.NTRIPLES.getName());
    TripleCommit.Diff diff = TripleCommit.Diff.fromString("+ ".concat(ntriple));
    diff.unapply(actual);
    Model expected = ModelFactory.createDefaultModel();
    assertTrue(expected.isIsomorphicWith(actual));
  }

  // @Test
  public void testReverseDiff() {
    String ntriple = "<info:subject> <info:predicate> <info:object> .";
    TripleCommit.Diff diff = TripleCommit.Diff.fromString("+ ".concat(ntriple)).reverse();
    assertEquals(1, diff.getLines().size());
    assertFalse(diff.getLines().get(0).add);
  }

  // @Test
  public void testNewlinesInLiterals() {
    String diffline = "+ <urn:uuid:706b2e06-77eb-11e5-9f9f-c48e8ff4ea31> <http://schema.org/description> \"UNIVERSITI" +
      " KEBANGSAAN MALAYSIA:  Motto, Vision, Mission & Philosophy\\r\\n\\r\\nMotto\u2028\\r\\nInspiring futures, " +
      "nurturing possibilities.\\r\\n\\r\\nPhilosophy\\r\\n\u2028UKM affirms the integration of faith in Allah and " +
      "constructive knowledge; along with the amalgamation of theory and practice as the core fundamentals in the " +
      "advancement of knowledge, the building of an educated society and the development of the university" +
      ".\u2028\u2028\\r\\n\\r\\nVision\u2028\\r\\nUKM is committed to be ahead  of society and time in leading the " +
      "development of  a learned, dynamic  and moral society.\\r\\n\\r\\nMission\u2028\\r\\n\\r\\nTo be the learning " +
      "centre of choice that  promotes the sovereignty of  Bahasa Melayu and internationalises knowledge rooted in " +
      "the national culture.\\r\\n\"@en .";
    TripleCommit.Diff diff = TripleCommit.Diff.fromString(diffline);
  }

  // @Test
  public void testBnodeRoundtrip() throws IOException {

    Model in = ModelFactory.createDefaultModel();
    RDFDataMgr.read(in, "TripleCommitTest/testBnodeRoundtrip.IN.1.nt", Lang.NTRIPLES);

    StmtIterator it = in.listStatements();
    TripleCommit.Diff diff = new TripleCommit.Diff();
    while (it.hasNext()) {
      diff.addStatement(it.next());
    }

    // Round trip
    String diffString = diff.toString();
    diff = TripleCommit.Diff.fromString(diffString);

    Model actual = ModelFactory.createDefaultModel();
    diff.apply(actual);
    assertTrue(actual.isIsomorphicWith(in));

  }

}
