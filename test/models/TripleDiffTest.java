package models;

import org.apache.jena.riot.RiotException;
import org.junit.Test;

/**
 * Created by fo on 11.02.16.
 */
public class TripleDiffTest {

  @Test
  public void testPlainLiteralLine() {
    TripleDiff tripleDiff = new TripleDiff();
    tripleDiff.fromString(" + <http://example.org/show/218> <http://www.w3.org/2000/01/rdf-schema#label> \"That Seventies Show\" .");
  }

  @Test
  public void testAddLanguageLiteralLine() {
    TripleDiff tripleDiff = new TripleDiff();
    tripleDiff.fromString("+ <http://example.org/show/218> <http://example.org/show/localName> \"That Seventies Show\"@en .");
  }

  @Test
  public void testAddTypedLiteralLine() {
    TripleDiff tripleDiff = new TripleDiff();
    tripleDiff.fromString("+ <http://en.wikipedia.org/wiki/Helium> <http://example.org/elements/specificGravity> \"1.663E-4\"^^<http://www.w3.org/2001/XMLSchema#double> .");
  }

  @Test(expected = RiotException.class)
  public void testInvalidLiteral() {
    TripleDiff tripleDiff = new TripleDiff();
    tripleDiff.fromString(" + <http://example.org/show/218> <http://www.w3.org/2000/01/rdf-schema#label> \"That Seventies Show .");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidOp() {
    TripleDiff tripleDiff = new TripleDiff();
    tripleDiff.fromString(" | <http://example.org/show/218> <http://www.w3.org/2000/01/rdf-schema#label> \"That Seventies Show\" .");
  }

}
